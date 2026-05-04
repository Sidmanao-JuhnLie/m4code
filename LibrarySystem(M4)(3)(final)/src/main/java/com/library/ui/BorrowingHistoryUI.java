package com.library.ui;

import com.library.model.Loan;
import com.library.model.User;
import com.library.service.LoanService;

import java.sql.SQLException;
import java.util.List;

public class BorrowingHistoryUI {

    private final LoanService loanService;
    private final User        user;

    public BorrowingHistoryUI(LoanService loanService, User user) {
        this.loanService = loanService;
        this.user        = user;
    }

    public void show() {
        try {
            List<Loan> history = loanService.getBorrowingHistory(user.getId());

            ConsoleUI.printHeader("VIEW BORROWING HISTORY");
            System.out.println();
            System.out.printf("  Member: %s [%s]%n", user.getDisplayName(), user.getMemberId());
            System.out.printf("  Total Borrows: %d%n%n", history.size());

            if (history.isEmpty()) {
                System.out.println("  No borrowing history yet.");
            } else {
                System.out.println("  " + ConsoleUI.DASH_62);
                System.out.printf("  %-4s | %-28s | %-10s | %-10s | %-8s%n",
                    "#", "Title", "Borrowed", "Returned", "Status");
                System.out.println("  " + ConsoleUI.DASH_62);
                for (int i = 0; i < history.size(); i++) {
                    Loan l = history.get(i);
                    String returned = l.getReturnedDate() != null ? l.getReturnedDate() : "-";
                    System.out.printf("  %-4d | %-28s | %-10s | %-10s | %-8s%n",
                        i + 1,
                        ConsoleUI.col(l.getBookTitle(), 28),
                        l.getBorrowedDate(),
                        returned,
                        l.getStatus());
                }
                System.out.println("  " + ConsoleUI.DASH_62);
            }

            ConsoleUI.pressEnter();

        } catch (SQLException e) {
            ConsoleUI.error("Database error: " + e.getMessage());
            ConsoleUI.pressEnter();
        }
    }
}
