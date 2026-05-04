package com.library.ui;

import com.library.model.Book;
import com.library.service.BookService;

import java.sql.SQLException;
import java.util.List;

public class BrowseCategoryUI {

    private final BookService bookService;

    public BrowseCategoryUI(BookService bookService) {
        this.bookService = bookService;
    }

    public void show() {
        while (true) {
            ConsoleUI.printHeader("BROWSE BOOK CATEGORIES");
            System.out.println();
            System.out.println("  Select a Category:");
            System.out.println();
            for (int i = 0; i < BookService.CATEGORIES.length; i++) {
                System.out.printf("    [%d] %s%n", i + 1, BookService.CATEGORIES[i]);
            }
            System.out.printf("    [%d] Back to Dashboard%n", BookService.CATEGORIES.length + 1);
            System.out.println();

            String choice = ConsoleUI.readLine("  Enter choice: ");
            try {
                int idx = Integer.parseInt(choice) - 1;
                if (idx == BookService.CATEGORIES.length) return;
                if (idx < 0 || idx >= BookService.CATEGORIES.length) {
                    ConsoleUI.error("Invalid choice."); continue;
                }

                String genre = BookService.CATEGORIES[idx];
                List<Book> books = bookService.browseByGenre(genre);

                System.out.printf("%n  Category: %s%n%n", genre);
                if (books.isEmpty()) {
                    System.out.println("  No books found in this category.");
                } else {
                    System.out.println("  " + ConsoleUI.DASH_62);
                    System.out.printf("  %-4s | %-36s | %-12s%n", "#", "Title", "Status");
                    System.out.println("  " + ConsoleUI.DASH_62);
                    for (int i = 0; i < books.size(); i++) {
                        Book b = books.get(i);
                        System.out.printf("  %-4d | %-36s | %-12s%n",
                            i + 1, ConsoleUI.col(b.getTitle(), 36), b.getStatus());
                    }
                    System.out.println("  " + ConsoleUI.DASH_62);

                    String sel = ConsoleUI.readLine("\n  Enter item # to view details [0] Back\n  Enter choice: ");
                    if (!sel.equals("0")) {
                        try {
                            int bookIdx = Integer.parseInt(sel) - 1;
                            if (bookIdx >= 0 && bookIdx < books.size())
                                new BookDetailUI(bookService).showBookDetail(books.get(bookIdx));
                            else
                                ConsoleUI.error("Invalid selection.");
                        } catch (NumberFormatException ignored) {}
                    }
                }
                ConsoleUI.pressEnter();

            } catch (NumberFormatException e) {
                ConsoleUI.error("Invalid input.");
            } catch (SQLException e) {
                ConsoleUI.error("Database error: " + e.getMessage());
                ConsoleUI.pressEnter();
            }
        }
    }
}
