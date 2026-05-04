package com.library.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Loan {
    private int id;
    private String loanId;
    private int userId;
    private int bookId;
    private String bookTitle;   // joined field
    private String borrowedDate;
    private String dueDate;
    private String returnedDate;
    private String returnId;
    private String status;      // Active | Returned | Overdue

    public Loan() {}

    public int getId()              { return id; }
    public String getLoanId()       { return loanId; }
    public int getUserId()          { return userId; }
    public int getBookId()          { return bookId; }
    public String getBookTitle()    { return bookTitle; }
    public String getBorrowedDate() { return borrowedDate; }
    public String getDueDate()      { return dueDate; }
    public String getReturnedDate() { return returnedDate; }
    public String getReturnId()     { return returnId; }
    public String getStatus()       { return status; }

    public void setId(int id)                       { this.id = id; }
    public void setLoanId(String loanId)            { this.loanId = loanId; }
    public void setUserId(int userId)               { this.userId = userId; }
    public void setBookId(int bookId)               { this.bookId = bookId; }
    public void setBookTitle(String bookTitle)      { this.bookTitle = bookTitle; }
    public void setBorrowedDate(String borrowedDate){ this.borrowedDate = borrowedDate; }
    public void setDueDate(String dueDate)          { this.dueDate = dueDate; }
    public void setReturnedDate(String returnedDate){ this.returnedDate = returnedDate; }
    public void setReturnId(String returnId)        { this.returnId = returnId; }
    public void setStatus(String status)            { this.status = status; }

    public String getDueSoonStatus() {
        try {
            LocalDate due  = LocalDate.parse(dueDate);
            LocalDate today = LocalDate.now();
            long days = ChronoUnit.DAYS.between(today, due);
            if (days < 0)  return "OVERDUE!";
            if (days <= 3) return "Due Soon!";
            return "On Time";
        } catch (Exception e) {
            return status;
        }
    }

    public String getTruncatedTitle(int maxLen) {
        if (bookTitle == null) return "";
        if (bookTitle.length() <= maxLen) return bookTitle;
        return bookTitle.substring(0, maxLen - 3) + "...";
    }
}
