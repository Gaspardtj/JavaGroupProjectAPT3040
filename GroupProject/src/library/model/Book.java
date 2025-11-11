package library.model;

import java.time.Year;

public class Book {
    private int bookId;
    private String isbn;
    private String title;
    private String author;
    private String genre;
    private int publishedYear;
    private int totalCopies;
    private int availableCopies;

    // Constructors
    public Book() {}

    public Book(String isbn, String title, String author, String genre, int publishedYear, int totalCopies) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.publishedYear = publishedYear;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    // Getters and Setters
    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getPublishedYear() { return publishedYear; }
    public void setPublishedYear(int publishedYear) { this.publishedYear = publishedYear; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    // Business methods
    public boolean isAvailable() {
        return availableCopies > 0;
    }

    public void borrowCopy() {
        if (availableCopies > 0) {
            availableCopies--;
        }
    }

    public void returnCopy() {
        if (availableCopies < totalCopies) {
            availableCopies++;
        }
    }

    public void displayInfo() {
        System.out.println("📚 " + title + " by " + author);
        System.out.println("   ISBN: " + isbn + " | Genre: " + genre + " | Year: " + publishedYear);
        System.out.println("   Available: " + availableCopies + "/" + totalCopies + " copies");
        System.out.println("   Status: " + (isAvailable() ? "AVAILABLE" : "UNAVAILABLE"));
    }
}