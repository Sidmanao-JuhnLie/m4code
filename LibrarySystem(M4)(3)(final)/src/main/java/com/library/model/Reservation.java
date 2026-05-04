package com.library.model;

public class Reservation {
    private int id;
    private String resId;
    private int userId;
    private int bookId;
    private String bookTitle;    // joined field
    private String resDate;
    private int queuePosition;
    private String status;       // Waiting | Ready for Pickup! | Borrowed | Cancelled

    /**
     * Transient field populated from JOIN with books.available_copies.
     * Used by ReservationStatusUI and BorrowUI to compute the dynamic
     * inventory-based status:
     *   availableCopies > 0  → "Ready to Pickup"
     *   availableCopies == 0 → "Waitlisted (Pending Stock)"
     */
    private int availableCopies = 0;

    public Reservation() {}

    public int getId()              { return id; }
    public String getResId()        { return resId; }
    public int getUserId()          { return userId; }
    public int getBookId()          { return bookId; }
    public String getBookTitle()    { return bookTitle; }
    public String getResDate()      { return resDate; }
    public int getQueuePosition()   { return queuePosition; }
    public String getStatus()       { return status; }

    public void setId(int id)                            { this.id = id; }
    public void setResId(String resId)                   { this.resId = resId; }
    public void setUserId(int userId)                    { this.userId = userId; }
    public void setBookId(int bookId)                    { this.bookId = bookId; }
    public void setBookTitle(String bookTitle)           { this.bookTitle = bookTitle; }
    public void setResDate(String resDate)               { this.resDate = resDate; }
    public void setQueuePosition(int queuePosition)      { this.queuePosition = queuePosition; }
    public void setStatus(String status)                 { this.status = status; }
    public void setAvailableCopies(int availableCopies)  { this.availableCopies = availableCopies; }

    public int getAvailableCopies() { return availableCopies; }

    /**
     * Computes the inventory-based display status per the task spec:
     *   availableCopies > 0  → "Ready to Pickup"
     *   availableCopies == 0 → "Waitlisted (Pending Stock)"
     */
    public String getDynamicStatus() {
        if (availableCopies > 0) {
            return "Ready to Pickup";
        } else {
            return "Waitlisted (Pending Stock)";
        }
    }

    public String getQueueDisplay() {
        return "#" + queuePosition;
    }

    public String getTruncatedTitle(int maxLen) {
        if (bookTitle == null) return "";
        if (bookTitle.length() <= maxLen) return bookTitle;
        return bookTitle.substring(0, maxLen - 3) + "...";
    }
}
