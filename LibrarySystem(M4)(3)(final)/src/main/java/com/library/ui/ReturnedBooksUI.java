package com.library.ui;

import com.library.model.Loan;
import com.library.model.User;
import com.library.service.LoanService;

import java.sql.SQLException;
import java.util.List;

public class ReturnedBooksUI {

    private final LoanService loanService;
    private final User        user;

    public ReturnedBooksUI(LoanService loanService, User user) {
        this.loanService = loanService;
        this.user        = user;
    }

    public void show() {
        try {
            List<Loan> returned = loanService.getReturnedLoans(user.getId());

            ConsoleUI.printHeader("VIEW RETURNED BOOKS");
            System.out.println();
            System.out.printf("  Member: %s [%s]%n", user.getDisplayName(), user.getMemberId());
            System.out.printf("  Total Returns: %d%n%n", returned.size());

            if (returned.isEmpty()) {
                System.out.println("  You have no returned books yet.");
            } else {
                System.out.println("  " + ConsoleUI.DASH_62);
                System.out.printf("  %-4s | %-28s | %-10s | %-10s | %-10s%n",
                    "#", "Title", "Borrowed", "Returned", "Return ID");
                System.out.println("  " + ConsoleUI.DASH_62);
                for (int i = 0; i < returned.size(); i++) {
                    Loan l = returned.get(i);
                    String returnId = l.getReturnId() != null ? l.getReturnId() : "-";
                    System.out.printf("  %-4d | %-28s | %-10s | %-10s | %-10s%n",
                        i + 1,
                        ConsoleUI.col(l.getBookTitle(), 28),
                        l.getBorrowedDate(),
                        l.getReturnedDate(),
                        returnId);
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
