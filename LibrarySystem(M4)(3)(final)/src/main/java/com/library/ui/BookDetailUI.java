package com.library.ui;

import com.library.model.Book;
import com.library.service.BookService;

import java.sql.SQLException;

public class BookDetailUI {

    private final BookService bookService;

    public BookDetailUI(BookService bookService) {
        this.bookService = bookService;
    }

    public void show() {
        ConsoleUI.printHeader("BOOK DETAILS");
        System.out.println();
        String bookIdInput = ConsoleUI.readLine("  Enter Book ID (e.g. BOOK-0001): ");

        try {
            Book book = bookService.findByBookId(bookIdInput);
            if (book == null) {
                ConsoleUI.error("Book not found with ID: " + bookIdInput);
                ConsoleUI.pressEnter();
                return;
            }
            showBookDetail(book);
        } catch (SQLException e) {
            ConsoleUI.error("Database error: " + e.getMessage());
            ConsoleUI.pressEnter();
        }
    }

    public void showBookDetail(Book book) {
        System.out.println("\n" + ConsoleUI.LINE_62);
        System.out.println("  BOOK DETAILS");
        System.out.println(ConsoleUI.LINE_62);
        System.out.println();
        System.out.printf("  Book ID        : %s%n", book.getBookId());
        System.out.printf("  Title          : %s%n", book.getTitle());
        System.out.printf("  Author         : %s%n", book.getAuthor());
        System.out.printf("  Genre          : %s%n", book.getGenre());
        System.out.printf("  Total Copies   : %d%n", book.getTotalCopies());
        System.out.printf("  Available      : %d%n", book.getAvailableCopies());
        System.out.printf("  Status         : %s%n", book.getStatus());
        System.out.println("\n  " + ConsoleUI.DASH_62);
        ConsoleUI.pressEnter();
    }
}
