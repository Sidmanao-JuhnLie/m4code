package com.library.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        try {
            createTables();
            seedBooks();
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void createTables() throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        Statement stmt = conn.createStatement();

        // Users table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                member_id   TEXT UNIQUE NOT NULL,
                email       TEXT UNIQUE NOT NULL,
                password    TEXT NOT NULL,
                name        TEXT NOT NULL,
                created_at  TEXT DEFAULT (datetime('now'))
            )
        """);

        // Books table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS books (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id          TEXT UNIQUE NOT NULL,
                title            TEXT NOT NULL,
                author           TEXT NOT NULL,
                genre            TEXT NOT NULL,
                total_copies     INTEGER NOT NULL DEFAULT 3,
                available_copies INTEGER NOT NULL DEFAULT 3
            )
        """);

        // Loans table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS loans (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                loan_id       TEXT UNIQUE NOT NULL,
                user_id       INTEGER NOT NULL,
                book_id       INTEGER NOT NULL,
                borrowed_date TEXT NOT NULL,
                due_date      TEXT NOT NULL,
                returned_date TEXT,
                return_id     TEXT,
                status        TEXT NOT NULL DEFAULT 'Active',
                FOREIGN KEY (user_id)  REFERENCES users(id),
                FOREIGN KEY (book_id)  REFERENCES books(id)
            )
        """);

        // Reservations table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS reservations (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                res_id         TEXT UNIQUE NOT NULL,
                user_id        INTEGER NOT NULL,
                book_id        INTEGER NOT NULL,
                res_date       TEXT NOT NULL,
                queue_position INTEGER NOT NULL DEFAULT 1,
                status         TEXT NOT NULL DEFAULT 'Waiting',
                FOREIGN KEY (user_id)  REFERENCES users(id),
                FOREIGN KEY (book_id)  REFERENCES books(id)
            )
        """);

        // Fee transactions table – persists payment records per user
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS fee_transactions (
                transaction_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id          INTEGER NOT NULL,
                book_title       TEXT    NOT NULL,
                transaction_type TEXT    NOT NULL,
                base_amount      REAL    NOT NULL DEFAULT 0.0,
                reservation_fee  REAL    NOT NULL DEFAULT 0.0,
                late_fee         REAL    NOT NULL DEFAULT 0.0,
                total_amount     REAL    NOT NULL,
                created_at       TEXT    DEFAULT (datetime('now')),
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """);

        stmt.close();
    }

    private static void seedBooks() throws SQLException {
        Connection conn = DatabaseConnection.getConnection();

        // Check if books already exist
        ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM books");
        if (rs.next() && rs.getInt(1) > 0) {
            rs.close();
            return;
        }
        rs.close();

        String[][] books = {
            {"BOOK-0001", "Java: The Complete Reference",  "Herbert Schildt",     "Science & Technology", "4", "4"},
            {"BOOK-0002", "Effective Java",                "Joshua Bloch",         "Science & Technology", "3", "2"},
            {"BOOK-0003", "Head First Java",               "Kathy Sierra",         "Science & Technology", "5", "5"},
            {"BOOK-0004", "Clean Code",                    "Robert C. Martin",     "Science & Technology", "4", "4"},
            {"BOOK-0005", "The Pragmatic Programmer",      "David Thomas",         "Science & Technology", "3", "3"},
            {"BOOK-0006", "Design Patterns",               "Gang of Four",         "Science & Technology", "2", "2"},
            {"BOOK-0007", "Introduction to Algorithms",    "Thomas H. Cormen",     "Science & Technology", "3", "3"},
            {"BOOK-0008", "The Great Gatsby",              "F. Scott Fitzgerald",  "Fiction",              "4", "4"},
            {"BOOK-0009", "To Kill a Mockingbird",         "Harper Lee",           "Fiction",              "5", "5"},
            {"BOOK-0010", "1984",                          "George Orwell",        "Fiction",              "4", "4"},
            {"BOOK-0011", "Brave New World",               "Aldous Huxley",        "Fiction",              "3", "3"},
            {"BOOK-0012", "The Hobbit",                    "J.R.R. Tolkien",       "Fiction",              "4", "4"},
            {"BOOK-0013", "Sapiens",                       "Yuval Noah Harari",    "Non-Fiction",          "4", "4"},
            {"BOOK-0014", "Atomic Habits",                 "James Clear",          "Non-Fiction",          "5", "5"},
            {"BOOK-0015", "Thinking, Fast and Slow",       "Daniel Kahneman",      "Non-Fiction",          "3", "3"},
            {"BOOK-0016", "A Brief History of Time",       "Stephen Hawking",      "History",              "3", "3"},
            {"BOOK-0017", "Guns, Germs, and Steel",        "Jared Diamond",        "History",              "2", "2"},
            {"BOOK-0018", "The Art of War",                "Sun Tzu",              "History",              "5", "5"},
            {"BOOK-0019", "Leonardo da Vinci",             "Walter Isaacson",      "Arts & Literature",    "3", "3"},
            {"BOOK-0020", "The Story of Art",              "E.H. Gombrich",        "Arts & Literature",    "2", "2"},
        };

        String sql = "INSERT INTO books (book_id, title, author, genre, total_copies, available_copies) VALUES (?,?,?,?,?,?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        for (String[] b : books) {
            ps.setString(1, b[0]);
            ps.setString(2, b[1]);
            ps.setString(3, b[2]);
            ps.setString(4, b[3]);
            ps.setInt(5, Integer.parseInt(b[4]));
            ps.setInt(6, Integer.parseInt(b[5]));
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }
}
