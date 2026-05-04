package com.library.repository;

import com.library.db.DatabaseConnection;
import com.library.model.FeeRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all SQL operations for the fee_transactions table.
 *
 * All queries use PreparedStatements to prevent SQL injection.
 *
 * INSERT is invoked from LibraryPaymentProcessor.finalizeTransaction()
 * so the record is saved at exactly the point the PaymentFramework
 * confirms a successful transaction.
 */
public class FeeTransactionRepository {

    // -----------------------------------------------------------------------
    // INSERT – called inside finalizeTransaction()
    // -----------------------------------------------------------------------

    /**
     * Persists a completed fee transaction for the given user.
     *
     * @param userId          the logged-in user's database ID
     * @param bookTitle       display title of the book
     * @param transactionType "BORROW" or "RETURN"
     * @param baseAmount      base book price before any fees
     * @param reservationFee  flat reservation fee (0 on return)
     * @param lateFee         overdue penalty (0 on borrow)
     * @param totalAmount     VAT-inclusive total from applyVAT()
     */
    public void insert(int    userId,
                       String bookTitle,
                       String transactionType,
                       double baseAmount,
                       double reservationFee,
                       double lateFee,
                       double totalAmount) throws SQLException {

        String sql = """
            INSERT INTO fee_transactions
                (user_id, book_title, transaction_type,
                 base_amount, reservation_fee, late_fee, total_amount)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,    userId);
            ps.setString(2, bookTitle);
            ps.setString(3, transactionType);
            ps.setDouble(4, baseAmount);
            ps.setDouble(5, reservationFee);
            ps.setDouble(6, lateFee);
            ps.setDouble(7, totalAmount);
            ps.executeUpdate();
        }
    }

    // -----------------------------------------------------------------------
    // SELECT – filtered by user ID for personalized history
    // -----------------------------------------------------------------------

    /**
     * Fetches all fee transaction records belonging to the specified user,
     * ordered by creation time (oldest first).
     *
     * @param userId the logged-in user's database ID
     * @return list of FeeRecord objects ready for display
     */
    public List<FeeRecord> findByUserId(int userId) throws SQLException {
        String sql = """
            SELECT book_title, transaction_type,
                   base_amount, reservation_fee, late_fee, total_amount,
                   created_at
            FROM   fee_transactions
            WHERE  user_id = ?
            ORDER  BY transaction_id ASC
        """;

        List<FeeRecord> records = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FeeRecord.TransactionType type =
                            rs.getString("transaction_type").equals("BORROW")
                            ? FeeRecord.TransactionType.BORROW
                            : FeeRecord.TransactionType.RETURN;

                    records.add(new FeeRecord(
                            rs.getString("book_title"),
                            type,
                            rs.getDouble("base_amount"),
                            rs.getDouble("reservation_fee"),
                            rs.getDouble("late_fee"),
                            rs.getDouble("total_amount"),
                            rs.getString("created_at")   // stored timestamp from DB
                    ));
                }
            }
        }
        return records;
    }

    // -----------------------------------------------------------------------
    // COUNT – used by FeeHistoryUI header
    // -----------------------------------------------------------------------

    /** Returns the total number of transactions recorded for this user. */
    public int countByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM fee_transactions WHERE user_id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
