package com.library.ui;

import com.library.model.Book;
import com.library.service.BookService;

import java.sql.SQLException;
import java.util.List;

public class SearchUI {

    private final BookService bookService;

    public SearchUI(BookService bookService) {
        this.bookService = bookService;
    }

    public void show() {
        while (true) {
            ConsoleUI.printHeader("SEARCH BOOKS");
            System.out.println();
            System.out.println("  Search by:");
            System.out.println("    [1] Title");
            System.out.println("    [2] Author Name");
            System.out.println("    [3] Back to Dashboard");
            System.out.println();

            String choice = ConsoleUI.readLine("  Enter choice: ");

            if (choice.equals("3")) return;
            if (!choice.equals("1") && !choice.equals("2")) {
                ConsoleUI.error("Invalid choice."); continue;
            }

            String keyword = ConsoleUI.readLine("  Enter keyword: ");

            try {
                List<Book> books = choice.equals("1")
                    ? bookService.searchByTitle(keyword)
                    : bookService.searchByAuthor(keyword);

                System.out.println();
                System.out.printf("  Searching catalog...%n");

                if (books.isEmpty()) {
                    System.out.printf("  0 result(s) found for: '%s'%n", keyword);
                    ConsoleUI.pressEnter();
                    continue;
                }

                System.out.printf("  %d result(s) found for: '%s'%n%n", books.size(), keyword);
                printBookTable(books);

                String sel = ConsoleUI.readLine("\n  Enter item # to view details [0] Back\n  Enter choice: ");
                if (!sel.equals("0")) {
                    try {
                        int idx = Integer.parseInt(sel) - 1;
                        if (idx >= 0 && idx < books.size())
                            new BookDetailUI(bookService).showBookDetail(books.get(idx));
                        else
                            ConsoleUI.error("Invalid selection.");
                    } catch (NumberFormatException e) {
                        ConsoleUI.error("Invalid input.");
                    }
                }

            } catch (SQLException e) {
                ConsoleUI.error("Database error: " + e.getMessage());
                ConsoleUI.pressEnter();
            }
        }
    }

    private void printBookTable(List<Book> books) {
        System.out.println("  " + ConsoleUI.DASH_62);
        System.out.printf("  %-4s | %-32s | %-14s | %-12s%n",
            "#", "Title", "Author", "Status");
        System.out.println("  " + ConsoleUI.DASH_62);
        for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            String author = b.getAuthor().length() > 14
                ? b.getAuthor().substring(0, 12) + ".." : b.getAuthor();
            System.out.printf("  %-4d | %-32s | %-14s | %-12s%n",
                i + 1,
                ConsoleUI.col(b.getTitle(), 32),
                author,
                b.getStatus());
        }
        System.out.println("  " + ConsoleUI.DASH_62);
    }
}
