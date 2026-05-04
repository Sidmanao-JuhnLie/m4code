package com.library.repository;

import com.library.db.DatabaseConnection;
import com.library.model.Loan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LoanRepository {

    public Loan createLoan(int userId, int bookId, String borrowedDate,
                           String dueDate) throws SQLException {
        String loanId = generateLoanId();
        String sql = """
            INSERT INTO loans (loan_id, user_id, book_id, borrowed_date, due_date, status)
            VALUES (?, ?, ?, ?, ?, 'Active')
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, loanId);
        ps.setInt(2, userId);
        ps.setInt(3, bookId);
        ps.setString(4, borrowedDate);
        ps.setString(5, dueDate);
        ps.executeUpdate();
        ps.close();

        ResultSet keys = DatabaseConnection.getConnection()
            .createStatement().executeQuery("SELECT last_insert_rowid()");
        int newId = keys.next() ? keys.getInt(1) : -1;
        keys.close();
        return findById(newId);
    }

    public Loan findByLoanId(String loanId) throws SQLException {
        String sql = """
            SELECT l.*, b.title as book_title
            FROM loans l JOIN books b ON l.book_id = b.id
            WHERE UPPER(l.loan_id) = UPPER(?)
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, loanId);
        List<Loan> list = executeQuery(ps);
        return list.isEmpty() ? null : list.get(0);
    }

    public Loan findById(int id) throws SQLException {
        String sql = """
            SELECT l.*, b.title as book_title
            FROM loans l JOIN books b ON l.book_id = b.id
            WHERE l.id = ?
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, id);
        List<Loan> list = executeQuery(ps);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Loan> findActiveByUserId(int userId) throws SQLException {
        String sql = """
            SELECT l.*, b.title as book_title
            FROM loans l JOIN books b ON l.book_id = b.id
            WHERE l.user_id = ? AND l.status = 'Active'
            ORDER BY l.due_date ASC
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, userId);
        return executeQuery(ps);
    }

    public List<Loan> findReturnedByUserId(int userId) throws SQLException {
        String sql = """
            SELECT l.*, b.title as book_title
            FROM loans l JOIN books b ON l.book_id = b.id
            WHERE l.user_id = ? AND l.status = 'Returned'
            ORDER BY l.returned_date DESC
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, userId);
        return executeQuery(ps);
    }

    public List<Loan> findAllByUserId(int userId) throws SQLException {
        String sql = """
            SELECT l.*, b.title as book_title
            FROM loans l JOIN books b ON l.book_id = b.id
            WHERE l.user_id = ?
            ORDER BY l.borrowed_date DESC
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, userId);
        return executeQuery(ps);
    }

    public boolean returnBook(String loanId, String returnedDate) throws SQLException {
        String returnId = generateReturnId();
        String sql = """
            UPDATE loans SET status = 'Returned', returned_date = ?, return_id = ?
            WHERE UPPER(loan_id) = UPPER(?) AND status = 'Active'
            """;
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, returnedDate);
        ps.setString(2, returnId);
        ps.setString(3, loanId);
        int rows = ps.executeUpdate();
        ps.close();
        return rows > 0;
    }

    public String getLastReturnId(String loanId) throws SQLException {
        String sql = "SELECT return_id FROM loans WHERE UPPER(loan_id) = UPPER(?)";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, loanId);
        ResultSet rs = ps.executeQuery();
        String returnId = rs.next() ? rs.getString("return_id") : null;
        rs.close(); ps.close();
        return returnId;
    }

    public boolean renewLoan(String loanId, String newDueDate) throws SQLException {
        String sql = "UPDATE loans SET due_date = ? WHERE UPPER(loan_id) = UPPER(?) AND status = 'Active'";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, newDueDate);
        ps.setString(2, loanId);
        int rows = ps.executeUpdate();
        ps.close();
        return rows > 0;
    }

    public int countActiveByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE user_id = ? AND status = 'Active'";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        int count = rs.next() ? rs.getInt(1) : 0;
        rs.close(); ps.close();
        return count;
    }

    public boolean hasActiveLoan(int userId, int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE user_id=? AND book_id=? AND status='Active'";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, bookId);
        ResultSet rs = ps.executeQuery();
        boolean has = rs.next() && rs.getInt(1) > 0;
        rs.close(); ps.close();
        return has;
    }

    private String generateLoanId() throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans";
        ResultSet rs = DatabaseConnection.getConnection().createStatement().executeQuery(sql);
        int count = rs.next() ? rs.getInt(1) : 0;
        rs.close();
        return String.format("LOAN-%04d", count + 1);
    }

    private String generateReturnId() throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE status = 'Returned'";
        ResultSet rs = DatabaseConnection.getConnection().createStatement().executeQuery(sql);
        int count = rs.next() ? rs.getInt(1) : 0;
        rs.close();
        return String.format("RET-%04d", count + 1);
    }

    private List<Loan> executeQuery(PreparedStatement ps) throws SQLException {
        ResultSet rs = ps.executeQuery();
        List<Loan> loans = new ArrayList<>();
        while (rs.next()) loans.add(mapRow(rs));
        rs.close(); ps.close();
        return loans;
    }

    private Loan mapRow(ResultSet rs) throws SQLException {
        Loan loan = new Loan();
        loan.setId(rs.getInt("id"));
        loan.setLoanId(rs.getString("loan_id"));
        loan.setUserId(rs.getInt("user_id"));
        loan.setBookId(rs.getInt("book_id"));
        loan.setBorrowedDate(rs.getString("borrowed_date"));
        loan.setDueDate(rs.getString("due_date"));
        loan.setReturnedDate(rs.getString("returned_date"));
        loan.setReturnId(rs.getString("return_id"));
        loan.setStatus(rs.getString("status"));
        try { loan.setBookTitle(rs.getString("book_title")); } catch (Exception ignored) {}
        return loan;
    }
}
