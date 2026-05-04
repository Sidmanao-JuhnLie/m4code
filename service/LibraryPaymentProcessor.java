package com.library.service;

import com.library.model.FeeRecord;
import com.library.repository.BookRepository;
import com.library.repository.FeeTransactionRepository;
import com.library.repository.ReservationRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Concrete implementation of PaymentFramework for library borrowing transactions.
 *
 * Fee schedule:
 *   Reservation Fee : $50.00  – added to baseAmount when a reserved book is borrowed
 *   Late Fee        : $10.00 per overdue day – added to baseAmount on return
 *   VAT             : 12% inclusive (applyVAT from PaymentFramework)
 *
 * SQL integration (both triggered inside finalizeTransaction):
 *   1. INSERT into fee_transactions – persists the VAT-inclusive total
 *   2. UPDATE reservations SET status = 'Borrowed' – atomic with payment success
 */
public class LibraryPaymentProcessor extends PaymentFramework {

    // -----------------------------------------------------------------------
    // Fee constants
    // -----------------------------------------------------------------------
    public static final double RESERVATION_FEE   = 50.00;
    public static final double LATE_FEE_PER_DAY  = 10.00;
    private static final double MIN_VALID_AMOUNT = 0.01;

    // -----------------------------------------------------------------------
    // Dependencies
    // -----------------------------------------------------------------------
    private final FeeTransactionRepository feeRepo  = new FeeTransactionRepository();
    private final ReservationRepository    resRepo  = new ReservationRepository();
    private final BookRepository           bookRepo = new BookRepository();

    // -----------------------------------------------------------------------
    // State set before processInvoice() — consumed inside finalizeTransaction()
    // -----------------------------------------------------------------------
    private int    currentUserId   = 0;
    private String currentTitle    = "";
    private String currentType     = "BORROW";
    private double currentBase     = 0.0;
    private double currentResvFee  = 0.0;
    private double currentLateFee  = 0.0;

    // Optional: reservation ID to mark as Borrowed on finalize (0 = none)
    private int    pendingResId    = 0;

    /**
     * Book's integer primary key, set by processBorrow() so finalizeTransaction()
     * can decrement AvailableCopies atomically with the fee INSERT and reservation
     * status update (task requirement: inventory update inside finalizeTransaction).
     */
    private int    currentBookId   = 0;

    // Captured after finalizeTransaction() for buildRecord()
    private double lastTotal       = 0.0;

    // -----------------------------------------------------------------------
    // Borrow (with optional reservation)
    // -----------------------------------------------------------------------

    /**
     * Processes a borrow transaction.
     *
     * If the book has an active reservation for this user, the reservation fee
     * is added to the base amount. On a successful finalizeTransaction():
     *   - the fee record is persisted to the DB
     *   - the reservation row is updated from 'Reserved' → 'Borrowed'
     *   - AvailableCopies is decremented in the books table
     *
     * @param userId          logged-in user's database ID
     * @param bookTitle       book title for display and history
     * @param bookBaseAmount  base book price (typically 0 for free libraries)
     * @param reservationFee  pass RESERVATION_FEE if reserved, 0.0 if not
     * @param reservationId   the reservation row ID to mark Borrowed (0 if none)
     * @param bookId          books.id primary key — used by finalizeTransaction()
     *                        to decrement AvailableCopies (task requirement)
     * @param discountPct     discount percentage (0-100)
     */
    public void processBorrow(int userId, String bookTitle,
                              double bookBaseAmount, double reservationFee,
                              int reservationId, int bookId, double discountPct) {
        this.currentUserId  = userId;
        this.currentTitle   = bookTitle;
        this.currentType    = "BORROW";
        this.currentBase    = bookBaseAmount;
        this.currentResvFee = reservationFee;
        this.currentLateFee = 0.0;
        this.pendingResId   = reservationId;
        this.currentBookId  = bookId;
        this.lastTotal      = 0.0;

        double baseAmount = bookBaseAmount + reservationFee;

        if (reservationFee > 0) {
            System.out.println();
            System.out.println("  Reservation Fee  : $" + String.format("%.2f", reservationFee));
        }

        processInvoice(baseAmount, discountPct);
    }

    // -----------------------------------------------------------------------
    // Return
    // -----------------------------------------------------------------------

    /**
     * Processes a return transaction.
     * Calculates overdue days, applies $10/day late fee if needed,
     * then finalizes directly — no discount, no VAT on returns.
     *
     * Console output:
     *   Base Amount: $XX.XX
     *   Charged: $XX.XX
     *
     * @param userId          logged-in user's database ID
     * @param bookTitle       book title for display and history
     * @param bookBaseAmount  base book price
     * @param dueDate         original due date string (yyyy-MM-dd)
     */
    public void processReturn(int userId, String bookTitle,
                              double bookBaseAmount, String dueDate) {
        LocalDate due      = LocalDate.parse(dueDate);
        LocalDate today    = LocalDate.now();
        long daysOverdue   = ChronoUnit.DAYS.between(due, today);
        double lateFee     = daysOverdue > 0 ? daysOverdue * LATE_FEE_PER_DAY : 0.0;

        this.currentUserId  = userId;
        this.currentTitle   = bookTitle;
        this.currentType    = "RETURN";
        this.currentBase    = bookBaseAmount;
        this.currentResvFee = 0.0;
        this.currentLateFee = lateFee;
        this.pendingResId   = 0;
        this.currentBookId  = 0;   // no inventory decrement on return (incrementAvailable is in LoanService)
        this.lastTotal      = 0.0;

        if (daysOverdue > 0) {
            System.out.println();
            System.out.println("  Days Overdue     : " + daysOverdue);
            System.out.println("  Late Fee         : $" + String.format("%.2f", lateFee)
                    + "  ($" + String.format("%.2f", LATE_FEE_PER_DAY)
                    + " x " + daysOverdue + " day(s))");
        } else {
            System.out.println();
            System.out.println("  No late fee - book returned on time.");
        }

        double totalAmount = bookBaseAmount + lateFee;

        System.out.println("Base Amount: $" + String.format("%.2f", totalAmount));

        if (validatePayment(totalAmount)) {
            finalizeTransaction(totalAmount);
        } else {
            System.out.println("--- Invoice Processing Failed: Payment Validation Rejected ---");
        }
    }

    // -----------------------------------------------------------------------
    // PaymentFramework abstract method implementations
    // -----------------------------------------------------------------------

    @Override
    protected boolean validatePayment(double totalAmount) {
        return totalAmount >= MIN_VALID_AMOUNT;
    }

    /**
     * Called by processInvoice() after validatePayment() passes.
     *
     * This is the single trigger point for all SQL side-effects:
     *   1. INSERT into fee_transactions with the VAT-inclusive total (applyVAT result)
     *   2. UPDATE reservations SET status='Borrowed' WHERE id=? AND user_id=?
     *
     * The totalAmount passed here is always the output of applyVAT(), satisfying
     * the 12% VAT-inclusive requirement from the task.
     */
    @Override
    protected void finalizeTransaction(double totalAmount) {
        this.lastTotal = totalAmount;
        System.out.println("  Charged: $" + String.format("%.2f", totalAmount));

        // ── 1. INSERT fee record (PreparedStatement, user-scoped) ─────────
        try {
            feeRepo.insert(
                    currentUserId,
                    currentTitle,
                    currentType,
                    currentBase,
                    currentResvFee,
                    currentLateFee,
                    totalAmount          // VAT-inclusive total from applyVAT()
            );
        } catch (SQLException e) {
            System.err.println("  [Warning] Fee record could not be saved: " + e.getMessage());
        }

        // ── 2. UPDATE reservation status to 'Borrowed' (if applicable) ───
        if (pendingResId > 0) {
            try {
                boolean updated = resRepo.markAsBorrowed(pendingResId, currentUserId);
                if (updated) {
                    System.out.println("  Reservation status updated to: Borrowed");
                }
            } catch (SQLException e) {
                System.err.println("  [Warning] Reservation status could not be updated: " + e.getMessage());
            }
        }

        // ── 3. DECREMENT AvailableCopies in books table ───────────────────
        // Per task spec: finalizeTransaction() is the single trigger point for
        // the inventory update, keeping fee payment and stock decrement atomic.
        if (currentType.equals("BORROW") && currentBookId > 0) {
            try {
                bookRepo.decrementAvailable(currentBookId);
                System.out.println("  Available copies decremented for book ID: " + currentBookId);
            } catch (SQLException e) {
                System.err.println("  [Warning] Available copies could not be decremented: " + e.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------------
    // buildRecord() – optional in-memory snapshot after a transaction
    // -----------------------------------------------------------------------

    public FeeRecord buildRecord() {
        if (lastTotal <= 0) return null;
        FeeRecord.TransactionType type = currentType.equals("BORROW")
                ? FeeRecord.TransactionType.BORROW
                : FeeRecord.TransactionType.RETURN;
        return new FeeRecord(currentTitle, type, currentBase,
                             currentResvFee, currentLateFee, lastTotal);
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------
    public double getLastTotal()  { return lastTotal; }
    public double getLateFee()    { return currentLateFee; }
}
