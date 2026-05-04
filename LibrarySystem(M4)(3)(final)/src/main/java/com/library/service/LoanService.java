package com.library.service;

import com.library.model.Book;
import com.library.model.Loan;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class LoanService {

    private static final int LOAN_DAYS   = 14;
    private static final int RENEW_DAYS  = 14;
    private static final int MAX_LOANS   = 5;

    private final LoanRepository loanRepo = new LoanRepository();
    private final BookRepository bookRepo = new BookRepository();

    public enum BorrowResult {
        SUCCESS, BOOK_NOT_FOUND, NO_COPIES, MAX_LOANS_REACHED,
        ALREADY_BORROWED, ERROR
    }

    public enum ReturnResult {
        SUCCESS, LOAN_NOT_FOUND, ALREADY_RETURNED,
        NOT_OWNER, ERROR
    }

    // -----------------------------------------------------------------------

    public BorrowResult borrowBook(int userId, String bookId) throws SQLException {
        Book book = bookRepo.findByBookId(bookId);
        if (book == null)                              return BorrowResult.BOOK_NOT_FOUND;
        if (book.getAvailableCopies() <= 0)            return BorrowResult.NO_COPIES;
        if (loanRepo.countActiveByUserId(userId) >= MAX_LOANS) return BorrowResult.MAX_LOANS_REACHED;
        if (loanRepo.hasActiveLoan(userId, book.getId()))       return BorrowResult.ALREADY_BORROWED;

        String today   = LocalDate.now().toString();
        String dueDate = LocalDate.now().plusDays(LOAN_DAYS).toString();

        // NOTE: bookRepo.decrementAvailable() is NOT called here.
        // Per task spec, the AvailableCopies decrement is performed inside
        // LibraryPaymentProcessor.finalizeTransaction() to keep inventory
        // update atomic with the fee record INSERT.
        loanRepo.createLoan(userId, book.getId(), today, dueDate);
        return BorrowResult.SUCCESS;
    }

    public Loan getLastLoan(int userId, String bookId) throws SQLException {
        Book book = bookRepo.findByBookId(bookId);
        if (book == null) return null;
        List<Loan> active = loanRepo.findActiveByUserId(userId);
        return active.stream()
            .filter(l -> l.getBookId() == book.getId())
            .findFirst().orElse(null);
    }

    public ReturnResult returnBook(int userId, String loanId) throws SQLException {
        Loan loan = loanRepo.findByLoanId(loanId);
        if (loan == null)                       return ReturnResult.LOAN_NOT_FOUND;
        if (!loan.getStatus().equals("Active")) return ReturnResult.ALREADY_RETURNED;
        if (loan.getUserId() != userId)         return ReturnResult.NOT_OWNER;

        String today = LocalDate.now().toString();
        boolean ok = loanRepo.returnBook(loanId, today);
        if (ok) bookRepo.incrementAvailable(loan.getBookId());
        return ok ? ReturnResult.SUCCESS : ReturnResult.ERROR;
    }

    public String getReturnId(String loanId) throws SQLException {
        return loanRepo.getLastReturnId(loanId);
    }

    public Loan findByLoanId(String loanId) throws SQLException {
        return loanRepo.findByLoanId(loanId);
    }

    public boolean renewLoan(String loanId, int userId) throws SQLException {
        Loan loan = loanRepo.findByLoanId(loanId);
        if (loan == null || loan.getUserId() != userId) return false;
        String newDue = LocalDate.parse(loan.getDueDate()).plusDays(RENEW_DAYS).toString();
        return loanRepo.renewLoan(loanId, newDue);
    }

    public List<Loan> getActiveLoans(int userId) throws SQLException {
        return loanRepo.findActiveByUserId(userId);
    }

    public List<Loan> getReturnedLoans(int userId) throws SQLException {
        return loanRepo.findReturnedByUserId(userId);
    }

    public List<Loan> getBorrowingHistory(int userId) throws SQLException {
        return loanRepo.findAllByUserId(userId);
    }
}
