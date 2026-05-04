package com.library.ui;

import com.library.model.Loan;
import com.library.model.User;
import com.library.service.FeeHistoryService;
import com.library.service.LibraryPaymentProcessor;
import com.library.service.LoanService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReturnUI {

    private final LoanService       loanService;
    private final User              user;
    private final FeeHistoryService feeHistoryService;   // kept for constructor compatibility

    public ReturnUI(LoanService loanService, User user, FeeHistoryService feeHistoryService) {
        this.loanService       = loanService;
        this.user              = user;
        this.feeHistoryService = feeHistoryService;
    }

    public void show() {
        ConsoleUI.printHeader("RETURN BOOKS");
        System.out.println();
        String loanId = ConsoleUI.readLine("  Enter Loan ID (e.g. LOAN-0001): ");

        try {
            Loan loan = loanService.findByLoanId(loanId);
            if (loan == null) {
                ConsoleUI.error("Loan not found with ID: " + loanId);
                ConsoleUI.pressEnter();
                return;
            }
            if (!loan.getStatus().equals("Active")) {
                ConsoleUI.error("This loan has already been returned.");
                ConsoleUI.pressEnter();
                return;
            }
            if (loan.getUserId() != user.getId()) {
                ConsoleUI.error("This loan does not belong to your account.");
                ConsoleUI.pressEnter();
                return;
            }

            System.out.println();
            System.out.printf("  Book     : %s%n", loan.getBookTitle());
            System.out.printf("  Borrowed : %s%n", loan.getBorrowedDate());
            System.out.printf("  Due Date : %s%n", loan.getDueDate());
            System.out.printf("  Returning: %s%n%n", LocalDate.now());

            String confirm = ConsoleUI.readConfirm("  Confirm return? [Y/N]: ");
            if (confirm.equals("N")) {
                ConsoleUI.info("Return cancelled.");
                ConsoleUI.pressEnter();
                return;
            }

            LoanService.ReturnResult result = loanService.returnBook(user.getId(), loanId);

            if (result == LoanService.ReturnResult.SUCCESS) {
                String returnId = loanService.getReturnId(loanId);
                ConsoleUI.success("Book returned successfully.");
                System.out.printf("  Return ID : %s%n", returnId);

                LocalDate due   = LocalDate.parse(loan.getDueDate());
                LocalDate today = LocalDate.now();
                long diff = ChronoUnit.DAYS.between(today, due);
                if (diff >= 0)
                    ConsoleUI.info(diff == 0
                        ? "Returned on time!"
                        : "Thank you for returning " + diff + " day(s) early!");
                else
                    ConsoleUI.info("Book was " + Math.abs(diff) + " day(s) overdue.");

                // ── Payment via PaymentFramework; INSERT fires in finalizeTransaction()
                System.out.println();
                LibraryPaymentProcessor payment = new LibraryPaymentProcessor();
                payment.processReturn(user.getId(), loan.getBookTitle(),
                                      0.00, loan.getDueDate());
            } else {
                ConsoleUI.error("Failed to process return. Please try again.");
            }
            ConsoleUI.pressEnter();

        } catch (SQLException e) {
            ConsoleUI.error("Database error: " + e.getMessage());
            ConsoleUI.pressEnter();
        }
    }
}
