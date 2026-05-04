package com.library.model;

public class Book {
    private int id;
    private String bookId;
    private String title;
    private String author;
    private String genre;
    private int totalCopies;
    private int availableCopies;

    public Book() {}

    public Book(int id, String bookId, String title, String author,
                String genre, int totalCopies, int availableCopies) {
        this.id = id;
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public int getId()               { return id; }
    public String getBookId()        { return bookId; }
    public String getTitle()         { return title; }
    public String getAuthor()        { return author; }
    public String getGenre()         { return genre; }
    public int getTotalCopies()      { return totalCopies; }
    public int getAvailableCopies()  { return availableCopies; }

    public void setId(int id)                          { this.id = id; }
    public void setBookId(String bookId)               { this.bookId = bookId; }
    public void setTitle(String title)                 { this.title = title; }
    public void setAuthor(String author)               { this.author = author; }
    public void setGenre(String genre)                 { this.genre = genre; }
    public void setTotalCopies(int totalCopies)        { this.totalCopies = totalCopies; }
    public void setAvailableCopies(int availableCopies){ this.availableCopies = availableCopies; }

    public String getStatus() {
        if (availableCopies <= 0) return "Checked Out";
        if (availableCopies == 1) return "Last Copy!";
        return "Available";
    }

    public String getTruncatedTitle(int maxLen) {
        if (title.length() <= maxLen) return title;
        return title.substring(0, maxLen - 3) + "...";
    }
}
