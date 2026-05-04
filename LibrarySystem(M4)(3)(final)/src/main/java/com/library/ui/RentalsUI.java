package com.library.ui;

import com.library.model.Loan;
import com.library.model.User;
import com.library.service.BookService;
import com.library.service.FeeHistoryService;
import com.library.service.LoanService;

import java.sql.SQLException;
import java.util.List;

public class RentalsUI {

    private final LoanService       loanService;
    private final BookService       bookService;
    private final User              user;
    private final FeeHistoryService feeHistoryService;

    public RentalsUI(LoanService loanService, BookService bookService,
                     User user, FeeHistoryService feeHistoryService) {
        this.loanService       = loanService;
        this.bookService       = bookService;
        this.user              = user;
        this.feeHistoryService = feeHistoryService;
    }

    public void show() {
        try {
            List<Loan> loans = loanService.getActiveLoans(user.getId());

            ConsoleUI.printHeader("VIEW BOOK RENTALS");
            System.out.println();
            System.out.printf("  Member: %s [%s]%n", user.getDisplayName(), user.getMemberId());
            System.out.printf("  Active Rentals: %d%n%n", loans.size());

            if (loans.isEmpty()) {
                System.out.println("  You have no active rentals.");
            } else {
                System.out.println("  " + ConsoleUI.DASH_62);
                System.out.printf("  %-4s | %-30s | %-10s | %-10s | %-10s%n",
                    "#", "Title", "Loan ID", "Due Date", "Status");
                System.out.println("  " + ConsoleUI.DASH_62);
                for (int i = 0; i < loans.size(); i++) {
                    Loan l = loans.get(i);
                    System.out.printf("  %-4d | %-30s | %-10s | %-10s | %-10s%n",
                        i + 1,
                        ConsoleUI.col(l.getBookTitle(), 30),
                        l.getLoanId(),
                        l.getDueDate(),
                        l.getDueSoonStatus());
                }
                System.out.println("  " + ConsoleUI.DASH_62);
            }

            System.out.println();
            System.out.println("  ACTIONS:");
            System.out.println("    [1] Renew a Loan");
            System.out.println("    [2] Return a Book");
            System.out.println("    [3] Back to Dashboard");
            System.out.println();

            String choice = ConsoleUI.readLine("  Enter choice: ");
            switch (choice) {
                case "1" -> renewLoan(loans);
                case "2" -> new ReturnUI(loanService, user, feeHistoryService).show();
                case "3" -> {}
                default  -> ConsoleUI.error("Invalid choice.");
            }

        } catch (SQLException e) {
            ConsoleUI.error("Database error: " + e.getMessage());
            ConsoleUI.pressEnter();
        }
    }

    private void renewLoan(List<Loan> loans) {
        if (loans.isEmpty()) {
            ConsoleUI.error("No active loans to renew.");
            ConsoleUI.pressEnter();
            return;
        }
        System.out.println();
        String loanId = ConsoleUI.readLine("  Enter Loan ID to renew: ");
        try {
            boolean ok = loanService.renewLoan(loanId, user.getId());
            if (ok) {
                Loan updated = loanService.findByLoanId(loanId);
                ConsoleUI.success("Loan renewed successfully!");
                if (updated != null)
                    System.out.printf("  New Due Date: %s%n", updated.getDueDate());
            } else {
                ConsoleUI.error("Could not renew loan. Check the Loan ID or ensure it's your active loan.");
            }
        } catch (SQLException e) {
            ConsoleUI.error("Database error: " + e.getMessage());
        }
        ConsoleUI.pressEnter();
    }
}
