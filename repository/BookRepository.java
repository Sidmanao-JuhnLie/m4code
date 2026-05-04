package com.library.repository;

import com.library.db.DatabaseConnection;
import com.library.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookRepository {

    public List<Book> searchByTitle(String keyword) throws SQLException {
        String sql = "SELECT * FROM books WHERE LOWER(title) LIKE LOWER(?) ORDER BY title";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, "%" + keyword + "%");
        return executeQuery(ps);
    }

    public List<Book> searchByAuthor(String keyword) throws SQLException {
        String sql = "SELECT * FROM books WHERE LOWER(author) LIKE LOWER(?) ORDER BY author";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, "%" + keyword + "%");
        return executeQuery(ps);
    }

    public List<Book> findByGenre(String genre) throws SQLException {
        String sql = "SELECT * FROM books WHERE genre = ? ORDER BY title";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, genre);
        return executeQuery(ps);
    }

    public Book findById(int id) throws SQLException {
        String sql = "SELECT * FROM books WHERE id = ?";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, id);
        List<Book> list = executeQuery(ps);
        return list.isEmpty() ? null : list.get(0);
    }

    public Book findByBookId(String bookId) throws SQLException {
        String sql = "SELECT * FROM books WHERE UPPER(book_id) = UPPER(?)";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, bookId);
        List<Book> list = executeQuery(ps);
        return list.isEmpty() ? null : list.get(0);
    }

    public void decrementAvailable(int bookId) throws SQLException {
        String sql = "UPDATE books SET available_copies = available_copies - 1 WHERE id = ? AND available_copies > 0";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, bookId);
        ps.executeUpdate();
        ps.close();
    }

    public void incrementAvailable(int bookId) throws SQLException {
        String sql = "UPDATE books SET available_copies = MIN(available_copies + 1, total_copies) WHERE id = ?";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, bookId);
        ps.executeUpdate();
        ps.close();
    }

    private List<Book> executeQuery(PreparedStatement ps) throws SQLException {
        ResultSet rs = ps.executeQuery();
        List<Book> books = new ArrayList<>();
        while (rs.next()) books.add(mapRow(rs));
        rs.close(); ps.close();
        return books;
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        return new Book(
            rs.getInt("id"),
            rs.getString("book_id"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("genre"),
            rs.getInt("total_copies"),
            rs.getInt("available_copies")
        );
    }
}
