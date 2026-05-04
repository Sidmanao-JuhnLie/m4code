package com.library.service;

import com.library.model.Book;
import com.library.model.Reservation;
import com.library.repository.BookRepository;
import com.library.repository.ReservationRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {

    private final ReservationRepository resRepo  = new ReservationRepository();
    private final BookRepository        bookRepo = new BookRepository();

    public enum ReserveResult {
        SUCCESS, BOOK_NOT_FOUND, ALREADY_RESERVED, ERROR
    }

    // -----------------------------------------------------------------------
    // Reserve
    // -----------------------------------------------------------------------

    public ReserveResult reserveBook(int userId, int bookId) throws SQLException {
        Book book = bookRepo.findById(bookId);
        if (book == null) return ReserveResult.BOOK_NOT_FOUND;
        if (resRepo.hasActiveReservation(userId, bookId)) return ReserveResult.ALREADY_RESERVED;

        String today = LocalDate.now().toString();
        resRepo.createReservation(userId, bookId, today);
        return ReserveResult.SUCCESS;
    }

    // -----------------------------------------------------------------------
    // Query – only statuses that are still "Reserved" (not Borrowed/Cancelled)
    // -----------------------------------------------------------------------

    /**
     * Returns reservations with status Waiting or Ready for Pickup!.
     * Borrowed and Cancelled are excluded so they never appear in the
     * View Reservation Status menu.
     */
    public List<Reservation> getActiveReservations(int userId) throws SQLException {
        return resRepo.findActiveByUserId(userId);
    }

    /**
     * Returns the active reservation a user holds for a specific book,
     * or null if none exists. Used by BorrowUI to detect reserved books.
     */
    public Reservation getActiveReservationForBook(int userId, int bookId) throws SQLException {
        return resRepo.findActiveByUserAndBook(userId, bookId);
    }

    // -----------------------------------------------------------------------
    // Status transition – called inside finalizeTransaction()
    // -----------------------------------------------------------------------

    /**
     * Marks the reservation as 'Borrowed' once the book is officially borrowed
     * and payment has been finalized. Uses user_id in the WHERE clause to
     * guarantee user-specific isolation.
     *
     * @param resId  the reservation's primary key
     * @param userId the logged-in user's ID
     * @return true if the row was updated successfully
     */
    public boolean markReservationAsBorrowed(int resId, int userId) throws SQLException {
        return resRepo.markAsBorrowed(resId, userId);
    }

    // -----------------------------------------------------------------------
    // Cancel
    // -----------------------------------------------------------------------

    public boolean cancelReservation(int resId, int userId) throws SQLException {
        return resRepo.cancelReservation(resId, userId);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    public boolean hasAvailableCopies(int bookId) throws SQLException {
        Book book = bookRepo.findById(bookId);
        return book != null && book.getAvailableCopies() > 0;
    }
}
