package com.library.repository;

import com.library.db.DatabaseConnection;
import com.library.model.Reservation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    public Reservation createReservation(int userId, int bookId,
                                         String resDate) throws SQLException {
        String resId    = generateResId();
        int    queuePos = getNextQueuePosition(bookId);
        String status   = queuePos == 1 ? "Ready for Pickup!" : "Waiting";

        String sql = """
            INSERT INTO reservations (res_id, user_id, book_id, res_date, queue_position, status)
            VALUES (?,?,?,?,?,?)
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, resId);
        ps.setInt(2, userId);
        ps.setInt(3, bookId);
        ps.setString(4, resDate);
        ps.setInt(5, queuePos);
        ps.setString(6, status);
        ps.executeUpdate();
        ps.close();

        ResultSet keys = DatabaseConnection.getConnection()
            .createStatement().executeQuery("SELECT last_insert_rowid()");
        int newId = keys.next() ? keys.getInt(1) : -1;
        keys.close();
        return findById(newId);
    }

    // -----------------------------------------------------------------------
    // READ – only "Reserved" statuses (Waiting / Ready for Pickup!)
    // Borrowed and Cancelled are intentionally excluded so they disappear
    // from the View Reservation Status screen automatically.
    // -----------------------------------------------------------------------

    public List<Reservation> findActiveByUserId(int userId) throws SQLException {
        // Task requirement: fetch available_copies from books table so the UI can
        // compute inventory-based status (Ready to Pickup / Waitlisted) per reserve.
        String sql = """
            SELECT r.*, b.title as book_title, b.available_copies
            FROM reservations r JOIN books b ON r.book_id = b.id
            WHERE r.user_id = ?
              AND r.status NOT IN ('Cancelled', 'Borrowed')
            ORDER BY r.res_date ASC
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, userId);
        return executeQuery(ps);
    }

    /**
     * Returns the active (non-cancelled, non-borrowed) reservation a user
     * holds for a specific book. Used by BorrowUI to detect reserved books.
     */
    public Reservation findActiveByUserAndBook(int userId, int bookId) throws SQLException {
        String sql = """
            SELECT r.*, b.title as book_title
            FROM reservations r JOIN books b ON r.book_id = b.id
            WHERE r.user_id = ?
              AND r.book_id = ?
              AND r.status NOT IN ('Cancelled', 'Borrowed')
            LIMIT 1
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, bookId);
        List<Reservation> list = executeQuery(ps);
        return list.isEmpty() ? null : list.get(0);
    }

    public Reservation findById(int id) throws SQLException {
        String sql = """
            SELECT r.*, b.title as book_title
            FROM reservations r JOIN books b ON r.book_id = b.id
            WHERE r.id = ?
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, id);
        List<Reservation> list = executeQuery(ps);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean hasActiveReservation(int userId, int bookId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE user_id = ? AND book_id = ?
              AND status NOT IN ('Cancelled', 'Borrowed')
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, bookId);
        ResultSet rs = ps.executeQuery();
        boolean has = rs.next() && rs.getInt(1) > 0;
        rs.close(); ps.close();
        return has;
    }

    // -----------------------------------------------------------------------
    // UPDATE – status transitions
    // -----------------------------------------------------------------------

    /**
     * Marks the reservation as 'Borrowed' once the book is officially borrowed.
     * Called inside finalizeTransaction() so the DB update is atomic with payment.
     * Uses WHERE user_id = ? to ensure user isolation.
     */
    public boolean markAsBorrowed(int resId, int userId) throws SQLException {
        String sql = """
            UPDATE reservations
            SET    status = 'Borrowed'
            WHERE  id      = ?
              AND  user_id = ?
              AND  status NOT IN ('Cancelled', 'Borrowed')
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, resId);
        ps.setInt(2, userId);
        int rows = ps.executeUpdate();
        ps.close();
        return rows > 0;
    }

    public boolean cancelReservation(int resId, int userId) throws SQLException {
        String sql = """
            UPDATE reservations SET status = 'Cancelled'
            WHERE id = ? AND user_id = ? AND status != 'Cancelled'
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, resId);
        ps.setInt(2, userId);
        int rows = ps.executeUpdate();
        ps.close();
        return rows > 0;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    public int countActiveByUserId(int userId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE user_id = ? AND status NOT IN ('Cancelled', 'Borrowed')
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        int count = rs.next() ? rs.getInt(1) : 0;
        rs.close(); ps.close();
        return count;
    }

    private int getNextQueuePosition(int bookId) throws SQLException {
        String sql = """
            SELECT COALESCE(MAX(queue_position), 0) + 1
            FROM reservations
            WHERE book_id = ? AND status NOT IN ('Cancelled', 'Borrowed')
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, bookId);
        ResultSet rs = ps.executeQuery();
        int pos = rs.next() ? rs.getInt(1) : 1;
        rs.close(); ps.close();
        return pos;
    }

    private String generateResId() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations";
        ResultSet rs = DatabaseConnection.getConnection().createStatement().executeQuery(sql);
        int count = rs.next() ? rs.getInt(1) : 0;
        rs.close();
        return String.format("RES-%04d", count + 1);
    }

    private List<Reservation> executeQuery(PreparedStatement ps) throws SQLException {
        ResultSet rs = ps.executeQuery();
        List<Reservation> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        rs.close(); ps.close();
        return list;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setResId(rs.getString("res_id"));
        r.setUserId(rs.getInt("user_id"));
        r.setBookId(rs.getInt("book_id"));
        r.setResDate(rs.getString("res_date"));
        r.setQueuePosition(rs.getInt("queue_position"));
        r.setStatus(rs.getString("status"));
        try { r.setBookTitle(rs.getString("book_title")); } catch (Exception ignored) {}
        // Inventory field: present only in queries that JOIN books with available_copies selected
        try { r.setAvailableCopies(rs.getInt("available_copies")); } catch (Exception ignored) {}
        return r;
    }
}
