package com.library.repository;

import com.library.db.DatabaseConnection;
import com.library.model.User;

import java.sql.*;

public class UserRepository {

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?)";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next() && rs.getInt(1) > 0;
        rs.close(); ps.close();
        return exists;
    }

    public User createUser(String email, String password, String name) throws SQLException {
        // Generate next member ID
        String memberId = generateMemberId();

        String sql = "INSERT INTO users (member_id, email, password, name) VALUES (?,?,?,?)";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, memberId);
        ps.setString(2, email);
        ps.setString(3, password);
        ps.setString(4, name);
        ps.executeUpdate();
        ps.close();

        ResultSet rs = DatabaseConnection.getConnection()
            .createStatement().executeQuery("SELECT last_insert_rowid()");
        int newId = rs.next() ? rs.getInt(1) : -1;
        rs.close();

        return findById(newId);
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        User user = rs.next() ? mapRow(rs) : null;
        rs.close(); ps.close();
        return user;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        User user = rs.next() ? mapRow(rs) : null;
        rs.close(); ps.close();
        return user;
    }

    private String generateMemberId() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        ResultSet rs = DatabaseConnection.getConnection().createStatement().executeQuery(sql);
        int count = rs.next() ? rs.getInt(1) : 0;
        rs.close();
        return String.format("M-%04d", count + 1);
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("member_id"),
            rs.getString("email"),
            rs.getString("password"),
            rs.getString("name"),
            rs.getString("created_at")
        );
    }
}
