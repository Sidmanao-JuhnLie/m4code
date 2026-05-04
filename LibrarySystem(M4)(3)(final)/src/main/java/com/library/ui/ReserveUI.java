package com.library.ui;

import com.library.model.Book;
import com.library.model.User;
import com.library.service.BookService;
import com.library.service.FeeHistoryService;
import com.library.service.LoanService;
import com.library.service.ReservationService;

import java.sql.SQLException;

public class ReserveUI {

    private final ReservationService reservationService;
    private final BookService        bookService;
    private final LoanService        loanService;
    private final User               user;
    private final FeeHistoryService  feeHistoryService;

    public ReserveUI(ReservationService reservationService,
                     BookService bookService,
                     LoanService loanService,
                     User user,
                     FeeHistoryService feeHistoryService) {
        this.reservationService = reservationService;
        this.bookService        = bookService;
        this.loanService        = loanService;
        this.user               = user;
        this.feeHistoryService  = feeHistoryService;
    }

    public void show() {
        ConsoleUI.printHeader("RESERVE BOOKS");
        System.out.println();
        String bookIdInput = ConsoleUI.readLine("  Enter Book ID (e.g. BOOK-0001): ");

        try {
            Book book = bookService.findByBookId(bookIdInput);
            if (book == null) {
                ConsoleUI.error("Book not found with ID: " + bookIdInput);
                ConsoleUI.pressEnter();
                return;
            }

            System.out.printf("%n  Selected: %s%n%n", book.getTitle());
            String confirm = ConsoleUI.readConfirm("  Confirm reservation for this book? [Y/N]: ");
            if (confirm.equals("N")) {
                ConsoleUI.info("Reservation cancelled.");
                ConsoleUI.pressEnter();
                return;
            }

            System.out.println("\n  Processing reservation...");

            ReservationService.ReserveResult result =
                reservationService.reserveBook(user.getId(), book.getId());

            switch (result) {
                case SUCCESS -> {
                    ConsoleUI.success("Reservation placed!");

                    // ── Verbatim message required by task ─────────────────
                    System.out.println();
                    System.out.println("  The reservation fee for this book will be charged");
                    System.out.println("  and paid once the book is officially borrowed.");
                    System.out.println();

                    if (reservationService.hasAvailableCopies(book.getId())) {
                        ConsoleUI.notice("Copies are available. Proceed to Borrow instead.");
                        System.out.println();
                        System.out.println("    [1] Borrow Now");
                        System.out.println("    [2] Reserve Anyway (keep reservation)");
                        System.out.println("    [3] Back");
                        System.out.println();
                        String sub = ConsoleUI.readLine("  Enter choice: ");
                        if (sub.equals("1")) {
                            new BorrowUI(loanService, bookService, user, feeHistoryService)
                                    .borrowByBook(book);
                            return;   // pressEnter handled inside borrowByBook
                        }
                    }
                }
                case ALREADY_RESERVED -> ConsoleUI.error("You already have an active reservation for this book.");
                case BOOK_NOT_FOUND   -> ConsoleUI.error("Book not found.");
                default               -> ConsoleUI.error("An error occurred.");
            }

            ConsoleUI.pressEnter();

        } catch (SQLException e) {
            ConsoleUI.error("Database error: " + e.getMessage());
            ConsoleUI.pressEnter();
        }
    }
}
