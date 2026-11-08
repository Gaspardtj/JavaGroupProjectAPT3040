package library.dao;

import library.model.Book;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {
    private final Connection connection;

    public BookDAO() {
        this.connection = library.dao.DatabaseConnection.getInstance().getConnection();
    }

    // CREATE - Add new book
    public boolean addBook(Book book) {
        String sql = "INSERT INTO books (isbn, title, author, genre, published_year, total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, book.getIsbn());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getAuthor());
            stmt.setString(4, book.getGenre());
            stmt.setInt(5, book.getPublishedYear());
            stmt.setInt(6, book.getTotalCopies());
            stmt.setInt(7, book.getAvailableCopies());

            int rowsInserted = stmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("❌ Error adding book: " + e.getMessage());
            return false;
        }
    }

    // READ - Get all books
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY title";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Book book = resultSetToBook(rs);
                books.add(book);
            }
        } catch (SQLException e) {
            System.out.println("❌ Error fetching books: " + e.getMessage());
        }
        return books;
    }

    // READ - Search books by title or author
    public List<Book> searchBooks(String keyword) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE title ILIKE ? OR author ILIKE ? ORDER BY title";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, "%" + keyword + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Book book = resultSetToBook(rs);
                books.add(book);
            }
        } catch (SQLException e) {
            System.out.println("❌ Error searching books: " + e.getMessage());
        }
        return books;
    }

    // UPDATE - Update book information
    public boolean updateBook(Book book) {
        String sql = "UPDATE books SET title=?, author=?, genre=?, published_year=?, total_copies=?, available_copies=? WHERE book_id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setString(3, book.getGenre());
            stmt.setInt(4, book.getPublishedYear());
            stmt.setInt(5, book.getTotalCopies());
            stmt.setInt(6, book.getAvailableCopies());
            stmt.setInt(7, book.getBookId());

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("❌ Error updating book: " + e.getMessage());
            return false;
        }
    }

    // Helper method to convert ResultSet to Book object
    private Book resultSetToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setBookId(rs.getInt("book_id"));
        book.setIsbn(rs.getString("isbn"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setGenre(rs.getString("genre"));
        book.setPublishedYear(rs.getInt("published_year"));
        book.setTotalCopies(rs.getInt("total_copies"));
        book.setAvailableCopies(rs.getInt("available_copies"));
        return book;
    }
}
