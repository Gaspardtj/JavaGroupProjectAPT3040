package library.dao;

import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Short-lived connections keep Swing workers independent of the legacy console connection. */
public class LibraryRepository {
    public void initialize() throws Exception {
        String schema = Files.readString(Paths.get("schema.sql"));
        try (Connection c = DatabaseConnection.openConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(false);
            try {
                for (String sql : schema.split(";")) if (!sql.trim().isEmpty()) s.execute(sql);
                c.commit();
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }

    public static class Snapshot {
        public final List<Object[]> books, members, loans;
        Snapshot(List<Object[]> books, List<Object[]> members, List<Object[]> loans) {
            this.books = books; this.members = members; this.loans = loans;
        }
    }

    public Snapshot load() throws SQLException {
        try (Connection c = DatabaseConnection.openConnection()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            c.setAutoCommit(false);
            Snapshot result = new Snapshot(
                query(c, "SELECT book_id,isbn,title,author,genre,published_year,total_copies,available_copies FROM books ORDER BY title"),
                query(c, "SELECT member_id,member_code,name,email,phone,membership_date,max_books_allowed,active_status FROM members ORDER BY name"),
                query(c, "SELECT l.loan_id,b.title,m.name,l.issued_date,l.due_date,l.returned_date,CASE WHEN l.returned_date IS NOT NULL THEN 'Returned' WHEN l.due_date < CURRENT_DATE THEN 'Overdue' ELSE 'On loan' END FROM loans l JOIN books b USING(book_id) JOIN members m USING(member_id) ORDER BY l.loan_id DESC"));
            c.commit();
            return result;
        }
    }
    private List<Object[]> query(Connection c, String sql) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            int count = r.getMetaData().getColumnCount();
            while (r.next()) {
                Object[] row = new Object[count];
                for (int i = 0; i < count; i++) row[i] = r.getObject(i + 1);
                rows.add(row);
            }
        }
        return rows;
    }
    public void saveBook(Integer id, String isbn, String title, String author, String genre, int year, int copies) throws SQLException {
        if (isbn.isBlank() || title.isBlank() || author.isBlank() || year < 1 || year > 9999 || copies < 0)
            throw new IllegalArgumentException("Enter an ISBN, title, author, valid year and non-negative copy count.");
        String sql = id == null
            ? "INSERT INTO books(isbn,title,author,genre,published_year,total_copies,available_copies) VALUES(?,?,?,?,?,?,?)"
            : "UPDATE books SET isbn=?,title=?,author=?,genre=?,published_year=?,available_copies=available_copies + ? - total_copies,total_copies=? WHERE book_id=? AND ? >= total_copies-available_copies";
        try (Connection c = DatabaseConnection.openConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1,isbn); s.setString(2,title); s.setString(3,author); s.setString(4,genre);
            s.setInt(5,year); s.setInt(6,copies); s.setInt(7,copies);
            if (id != null) { s.setInt(8,id); s.setInt(9,copies); }
            if (s.executeUpdate() != 1) throw new IllegalArgumentException("Total copies cannot be lower than the number currently on loan. Refresh and try again.");
        }
    }
    public void saveMember(Integer id, String name, String email, String phone, int limit, boolean active) throws SQLException {
        if (name.isBlank() || limit < 1) throw new IllegalArgumentException("Enter a member name and a borrowing limit of at least 1.");
        if (!email.isBlank() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Enter a valid email address or leave it blank.");
        String sql = id == null
            ? "INSERT INTO members(name,email,phone,max_books_allowed,active_status,member_code) VALUES(?,?,?,?,?,?)"
            : "UPDATE members SET name=?,email=?,phone=?,max_books_allowed=?,active_status=? WHERE member_id=?";
        try (Connection c = DatabaseConnection.openConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1,name); s.setString(2,email); s.setString(3,phone); s.setInt(4,limit); s.setBoolean(5,active);
            if (id == null) s.setString(6,"MEM-" + UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT));
            else s.setInt(6,id);
            if (s.executeUpdate() != 1) throw new IllegalArgumentException("This member no longer exists. Refresh and try again.");
        }
    }
    public void issueBook(int bookId, int memberId, LocalDate due) throws SQLException {
        if (due.isBefore(LocalDate.now())) throw new IllegalArgumentException("The due date cannot be in the past.");
        try (Connection c = DatabaseConnection.openConnection()) {
            c.setAutoCommit(false);
            try {
                int limit;
                try (PreparedStatement s = c.prepareStatement("SELECT active_status,max_books_allowed FROM members WHERE member_id=? FOR UPDATE")) {
                    s.setInt(1,memberId);
                    try (ResultSet r = s.executeQuery()) {
                        if (!r.next() || !r.getBoolean(1)) throw new IllegalArgumentException("Select an active member.");
                        limit = r.getInt(2);
                    }
                }
                try (PreparedStatement s = c.prepareStatement("SELECT count(*) FROM loans WHERE member_id=? AND returned_date IS NULL")) {
                    s.setInt(1,memberId);
                    try (ResultSet r = s.executeQuery()) {
                        r.next(); if (r.getInt(1) >= limit) throw new IllegalArgumentException("This member has reached their borrowing limit.");
                    }
                }
                try (PreparedStatement s = c.prepareStatement("UPDATE books SET available_copies=available_copies-1 WHERE book_id=? AND available_copies>0")) {
                    s.setInt(1,bookId);
                    if (s.executeUpdate() != 1) throw new IllegalArgumentException("No copies of this book are available.");
                }
                try (PreparedStatement s = c.prepareStatement("INSERT INTO loans(book_id,member_id,due_date) VALUES(?,?,?)")) {
                    s.setInt(1,bookId); s.setInt(2,memberId); s.setDate(3,java.sql.Date.valueOf(due)); s.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }
    public void returnBook(int loanId) throws SQLException {
        try (Connection c = DatabaseConnection.openConnection()) {
            c.setAutoCommit(false);
            try {
                int bookId;
                try (PreparedStatement s = c.prepareStatement("UPDATE loans SET returned_date=CURRENT_DATE WHERE loan_id=? AND returned_date IS NULL RETURNING book_id")) {
                    s.setInt(1,loanId);
                    try (ResultSet r = s.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("This loan has already been returned. Refresh the list.");
                        bookId = r.getInt(1);
                    }
                }
                try (PreparedStatement s = c.prepareStatement("UPDATE books SET available_copies=available_copies+1 WHERE book_id=?")) {
                    s.setInt(1,bookId); s.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }
}
