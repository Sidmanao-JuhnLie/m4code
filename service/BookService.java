package com.library.service;

import com.library.model.Book;
import com.library.repository.BookRepository;

import java.sql.SQLException;
import java.util.List;

public class BookService {

    private final BookRepository bookRepo = new BookRepository();

    public List<Book> searchByTitle(String keyword) throws SQLException {
        return bookRepo.searchByTitle(keyword);
    }

    public List<Book> searchByAuthor(String keyword) throws SQLException {
        return bookRepo.searchByAuthor(keyword);
    }

    public List<Book> browseByGenre(String genre) throws SQLException {
        return bookRepo.findByGenre(genre);
    }

    public Book findById(int id) throws SQLException {
        return bookRepo.findById(id);
    }

    public Book findByBookId(String bookId) throws SQLException {
        return bookRepo.findByBookId(bookId);
    }

    public static final String[] CATEGORIES = {
        "Fiction",
        "Non-Fiction",
        "Science & Technology",
        "History",
        "Arts & Literature"
    };
}
