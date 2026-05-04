package com.library.ui;

import com.library.model.User;
import com.library.service.*;

public class Dashboard {

    private final AuthService        authService;
    private final BookService        bookService        = new BookService();
    private final LoanService        loanService        = new LoanService();
    private final ReservationService reservationService = new ReservationService();
    private final FeeHistoryService  feeHistoryService  = new FeeHistoryService();

    public Dashboard(AuthService authService) {
        this.authService = authService;
    }

    public void show() {
        User user = authService.getCurrentUser();
        boolean running = true;

        while (running) {
            printMenu(user);
            String choice = ConsoleUI.readLine("  Enter choice: ");

            switch (choice) {
                case "1"  -> new SearchUI(bookService).show();
                case "2"  -> new BookDetailUI(bookService).show();
                case "3"  -> new BrowseCategoryUI(bookService).show();
                case "4"  -> new ReserveUI(reservationService, bookService, loanService, user, feeHistoryService).show();
                case "5"  -> new BorrowUI(loanService, bookService, user, feeHistoryService).show();
                case "6"  -> new ReturnUI(loanService, user, feeHistoryService).show();
                case "7"  -> new RentalsUI(loanService, bookService, user, feeHistoryService).show();
                case "8"  -> new ReservationStatusUI(reservationService, loanService, bookService, feeHistoryService, user).show();
                case "9"  -> new BorrowingHistoryUI(loanService, user).show();
                case "10" -> new ReturnedBooksUI(loanService, user).show();
                case "11" -> new FeeHistoryUI(feeHistoryService, user).show();
                case "12" -> { authService.logout(); running = false; }
                default   -> ConsoleUI.error("Invalid choice. Please enter 1-12.");
            }
        }

        System.out.println("\n  You have been logged out. Goodbye!");
    }

    private void printMenu(User user) {
        System.out.println("\n" + ConsoleUI.LINE_62);
        System.out.printf("  Welcome! %s  [%s]%n", user.getDisplayName(), user.getMemberId());
        System.out.println(ConsoleUI.LINE_62);
        System.out.println();
        System.out.println("  MENU:");
        System.out.println("    [1]  Search Books");
        System.out.println("    [2]  View Book Details");
        System.out.println("    [3]  Browse Book Categories");
        System.out.println("    [4]  Reserve Books");
        System.out.println("    [5]  Borrow Books");
        System.out.println("    [6]  Return Books");
        System.out.println("    [7]  View Book Rentals");
        System.out.println("    [8]  View Reservation Status");
        System.out.println("    [9]  View Borrowing History");
        System.out.println("    [10] View Returned Books");
        System.out.println("    [11] Fee History");
        System.out.println("    [12] Log Out");
        System.out.println();
    }
}
