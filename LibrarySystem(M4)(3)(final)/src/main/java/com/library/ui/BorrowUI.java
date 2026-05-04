package com.library.ui;

import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Reservation;
import com.library.model.User;
import com.library.service.BookService;
import com.library.service.FeeHistoryService;
import com.library.service.LibraryPaymentProcessor;
import com.library.service.LoanService;
import com.library.service.ReservationService;

import java.sql.SQLException;
import java.time.LocalDate;

public class BorrowUI {

    private final LoanService        loanService;
    private final BookService        bookService;
    private final ReservationService reservationService;
    private final User               user;
    private final FeeHistoryService  feeHistoryService;   // kept for constructor compatibility

    public BorrowUI(LoanService loanService, BookService bookService,
                    User user, FeeHistoryService feeHistoryService) {
        this.loanService        = loanService;
        this.bookService        = bookService;
        this.reservationService = new ReservationService();
        this.user               = user;
        this.feeHistoryService  = feeHistoryService;
    }

    public void show() {
        ConsoleUI.printHeader("BORROW BOOKS");
        System.out.println();
        String bookIdInput = ConsoleUI.readLine("  Enter Book ID (e.g. BOOK-0001): ");
        try {
            Book book = bookService.findByBookId(bookIdInput);
            if (book == null) {
                ConsoleUI.error("Book not found with ID: " + bookIdInput);
                ConsoleUI.pressEnter();
                return;
            }
            borrowByBook(book);
        } catch (SQLException e) {
            ConsoleUI.error("Database error: " + e.getMessage());
            ConsoleUI.pressEnter();
        }
    }

    public void borrowByBook(Book book) throws SQLException {
        System.out.println();
        System.out.printf("  Book Found       : %s%n", book.getTitle());
        System.out.printf("  Available Copies : %d%n", book.getAvailableCopies());

        // ── Inventory-based dynamic status (if-else on AvailableCopies, per task spec) ──
        String dynamicStatus;
        if (book.getAvailableCopies() > 0) {
            dynamicStatus = "Ready to Pickup";
        } else {
            dynamicStatus = "Waitlisted (Pending Stock)";
        }
        System.out.printf("  Book Status      : %s%n", dynamicStatus);

        // ── Borrowing restriction: only "Ready to Pickup" may proceed ──
        if (!dynamicStatus.equals("Ready to Pickup")) {
            ConsoleUI.error("This book is Waitlisted (Pending Stock).");
            System.out.println("  Borrow is only permitted when the status is \"Ready to Pickup\".");
            System.out.println("  Please reserve this book or wait until copies are available.");
            ConsoleUI.pressEnter();
            return;
        }

        // ── Check for an active reservation for this user + book ──────────
        Reservation reservation = reservationService
                .getActiveReservationForBook(user.getId(), book.getId());

        double reservationFee = 0.0;
        int    reservationId  = 0;

        if (reservation != null) {
            reservationFee = LibraryPaymentProcessor.RESERVATION_FEE;
            reservationId  = reservation.getId();
            ConsoleUI.info("You have a reservation for this book.");
            System.out.printf("  Reservation ID   : %s%n", reservation.getResId());
            System.out.printf("  Reservation Fee  : $%.2f (charged on borrow)%n", reservationFee);
        }

        String dueDate = LocalDate.now().plusDays(14).toString();
        System.out.printf("  Due Date         : %s%n%n", dueDate);

        String confirm = ConsoleUI.readConfirm("  Confirm borrow? [Y/N]: ");
        if (confirm.equals("N")) {
            ConsoleUI.info("Borrow cancelled.");
            ConsoleUI.pressEnter();
            return;
        }

        System.out.println("\n  Processing checkout...");

        LoanService.BorrowResult result = loanService.borrowBook(user.getId(), book.getBookId());

        switch (result) {
            case SUCCESS -> {
                Loan loan = loanService.getLastLoan(user.getId(), book.getBookId());
                ConsoleUI.success("Book checked out!");
                if (loan != null) {
                    System.out.printf("  Due Date  : %s%n", loan.getDueDate());
                    System.out.printf("  Loan ID   : %s%n", loan.getLoanId());
                }

                // ── Payment via PaymentFramework ──────────────────────────
                // finalizeTransaction() will:
                //   1. INSERT fee record into fee_transactions
                //   2. UPDATE reservation status to 'Borrowed' (if reserved)
                //   3. DECREMENT available_copies in books (inventory update)
                System.out.println();
                LibraryPaymentProcessor payment = new LibraryPaymentProcessor();
                payment.processBorrow(
                        user.getId(),
                        book.getTitle(),
                        0.00,             // base book price
                        reservationFee,   // $50 if reserved, $0 if not
                        reservationId,    // res row ID to mark Borrowed (0 = none)
                        book.getId(),     // bookId for AvailableCopies decrement
                        5.0               // 5% discount applied to baseAmount
                );
            }
            case NO_COPIES         -> ConsoleUI.error("No copies available.");
            case MAX_LOANS_REACHED -> ConsoleUI.error("You have reached the maximum of 5 active loans.");
            case ALREADY_BORROWED  -> ConsoleUI.error("You already have an active loan for this book.");
            default                -> ConsoleUI.error("An error occurred.");
        }
        ConsoleUI.pressEnter();
    }
}
