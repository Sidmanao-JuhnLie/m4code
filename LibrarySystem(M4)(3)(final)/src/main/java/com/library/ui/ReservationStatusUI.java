package com.library.ui;

import com.library.model.Book;
import com.library.model.Reservation;
import com.library.model.User;
import com.library.service.BookService;
import com.library.service.FeeHistoryService;
import com.library.service.LoanService;
import com.library.service.ReservationService;

import java.sql.SQLException;
import java.util.List;

/**
 * Displays a user's active reservations with INVENTORY-BASED dynamic status.
 *
 * Status derivation (per task spec, using if-else on AvailableCopies from DB):
 *   if (availableCopies > 0)  → "Ready to Pickup"
 *   if (availableCopies == 0) → "Waitlisted (Pending Stock)"
 *
 * The stored status column ('Waiting' / 'Ready for Pickup!') is used only for
 * DB-level filtering; the DISPLAYED status is always computed from inventory.
 *
 * Borrow action: users may borrow a reserved book only when its dynamic status
 * is "Ready to Pickup". Selecting a "Waitlisted" book shows an error instead.
 */
public class ReservationStatusUI {

    private final ReservationService reservationService;
    private final LoanService        loanService;
    private final BookService        bookService;
    private final FeeHistoryService  feeHistoryService;
    private final User               user;

    public ReservationStatusUI(ReservationService reservationService,
                                LoanService loanService,
                                BookService bookService,
                                FeeHistoryService feeHistoryService,
                                User user) {
        this.reservationService = reservationService;
        this.loanService        = loanService;
        this.bookService        = bookService;
        this.feeHistoryService  = feeHistoryService;
        this.user               = user;
    }

    public void show() {
        try {
            // ── 1. Fetch active reservations (includes b.available_copies via JOIN) ──
            List<Reservation> reservations =
                    reservationService.getActiveReservations(user.getId());

            ConsoleUI.printHeader("VIEW RESERVATION STATUS");
            System.out.println();
            System.out.printf("  Member: %s [%s]%n", user.getDisplayName(), user.getMemberId());
            System.out.printf("  Active Reservations: %d%n%n", reservations.size());

            if (reservations.isEmpty()) {
                System.out.println("  You have no active reservations.");
            } else {
                System.out.println("  " + ConsoleUI.DASH_62);
                System.out.printf("  %-4s | %-24s | %-10s | %-6s | %-24s%n",
                    "#", "Title", "Res. Date", "Queue", "Status");
                System.out.println("  " + ConsoleUI.DASH_62);

                for (int i = 0; i < reservations.size(); i++) {
                    Reservation r = reservations.get(i);

                    // ── 2. Dynamic status: if-else on AvailableCopies (task spec) ──
                    String displayStatus;
                    if (r.getAvailableCopies() > 0) {
                        displayStatus = "Ready to Pickup";
                    } else {
                        displayStatus = "Waitlisted (Pending Stock)";
                    }

                    System.out.printf("  %-4d | %-24s | %-10s | %-6s | %-24s%n",
                        i + 1,
                        ConsoleUI.col(r.getBookTitle(), 24),
                        r.getResDate(),
                        r.getQueueDisplay(),
                        displayStatus);
                }
                System.out.println("  " + ConsoleUI.DASH_62);
                System.out.println();
                System.out.println("  Status Key:");
                System.out.println("    Ready to Pickup            - Copies available; you may Borrow");
                System.out.println("    Waitlisted (Pending Stock) - No copies; borrow blocked");
            }

            System.out.println();
            System.out.println("  ACTIONS:");
            System.out.println("    [1] Cancel a Reservation");
            System.out.println("    [2] Borrow a Reserved Book  (Ready to Pickup only)");
            System.out.println("    [3] Back to Dashboard");
            System.out.println();

            String choice = ConsoleUI.readLine("  Enter choice: ");
            switch (choice) {
                case "1" -> cancelReservation(reservations);
                case "2" -> borrowReservedBook(reservations);
            }

        } catch (SQLException e) {
            ConsoleUI.error("Database error: " + e.getMessage());
            ConsoleUI.pressEnter();
        }
    }

    // -----------------------------------------------------------------------
    // Cancel
    // -----------------------------------------------------------------------

    private void cancelReservation(List<Reservation> reservations) {
        if (reservations.isEmpty()) {
            ConsoleUI.error("No active reservations to cancel.");
            ConsoleUI.pressEnter();
            return;
        }
        System.out.println();
        String input = ConsoleUI.readLine("  Enter # of reservation to cancel: ");
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx < 0 || idx >= reservations.size()) {
                ConsoleUI.error("Invalid selection.");
                ConsoleUI.pressEnter();
                return;
            }
            Reservation r = reservations.get(idx);
            String confirm = ConsoleUI.readConfirm(
                "  Cancel reservation for \"" + r.getBookTitle() + "\"? [Y/N]: ");
            if (confirm.equals("N")) {
                ConsoleUI.info("Cancellation aborted.");
                ConsoleUI.pressEnter();
                return;
            }

            boolean ok = reservationService.cancelReservation(r.getId(), user.getId());
            if (ok) ConsoleUI.success("Reservation cancelled.");
            else    ConsoleUI.error("Failed to cancel reservation.");

        } catch (NumberFormatException e) {
            ConsoleUI.error("Invalid input.");
        } catch (SQLException e) {
            ConsoleUI.error("Database error: " + e.getMessage());
        }
        ConsoleUI.pressEnter();
    }

    // -----------------------------------------------------------------------
    // Borrow from reservation list
    // -----------------------------------------------------------------------

    /**
     * Borrowing restriction — enforced with explicit if-else on AvailableCopies:
     *
     *   if (availableCopies > 0)  status = "Ready to Pickup"  → allow borrow
     *   if (availableCopies == 0) status = "Waitlisted"       → BLOCK borrow
     *
     * Only "Ready to Pickup" reservations may trigger the reservation fee and
     * proceed into BorrowUI, satisfying task requirement 1 (Borrowing Restriction).
     */
    private void borrowReservedBook(List<Reservation> reservations) {
        if (reservations.isEmpty()) {
            ConsoleUI.error("No active reservations to borrow.");
            ConsoleUI.pressEnter();
            return;
        }
        System.out.println();
        String input = ConsoleUI.readLine("  Enter # of reservation to borrow: ");
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx < 0 || idx >= reservations.size()) {
                ConsoleUI.error("Invalid selection.");
                ConsoleUI.pressEnter();
                return;
            }

            Reservation r = reservations.get(idx);

            // ── Inventory-based borrowing restriction (if-else on AvailableCopies) ──
            if (r.getAvailableCopies() > 0) {
                // Dynamic status = "Ready to Pickup" → borrow is permitted
                Book book = bookService.findById(r.getBookId());
                if (book == null) {
                    ConsoleUI.error("Book record not found.");
                    ConsoleUI.pressEnter();
                    return;
                }
                new BorrowUI(loanService, bookService, user, feeHistoryService)
                        .borrowByBook(book);
                // pressEnter() is handled inside BorrowUI.borrowByBook()

            } else {
                // Dynamic status = "Waitlisted (Pending Stock)" → borrow is blocked
                ConsoleUI.error(
                    "\"" + r.getBookTitle() + "\" is currently Waitlisted (Pending Stock).");
                System.out.println("  Borrow is only allowed when the status is \"Ready to Pickup\".");
                System.out.println("  Please wait until a copy is returned to the library.");
                ConsoleUI.pressEnter();
            }

        } catch (NumberFormatException e) {
            ConsoleUI.error("Invalid input.");
            ConsoleUI.pressEnter();
        } catch (SQLException e) {
            ConsoleUI.error("Database error: " + e.getMessage());
            ConsoleUI.pressEnter();
        }
    }
}
