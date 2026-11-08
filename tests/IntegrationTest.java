import library.dao.*;
import library.model.*;
import library.service.LibraryService;
import java.sql.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class IntegrationTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Connection c = DatabaseConnection.getInstance().getConnection();
        c.setAutoCommit(false);
        String id = "test-" + UUID.randomUUID();
        try {
            BookDAO books = new BookDAO();
            MemberDAO members = new MemberDAO();
            Book book = new Book(id, "MixedCase " + id, "Test Author", "Test", 2020, 2);
            check(books.addBook(book), "Book insert failed");
            check(books.searchBooks("mixedcase " + id).size() == 1, "Case-insensitive search failed");
            Book saved = books.searchBooks(id).get(0);
            saved.borrowCopy();
            check(books.updateBook(saved), "Book update failed");
            check(books.searchBooks(id).get(0).getAvailableCopies() == 1, "Copy count not persisted");
            check(members.addMember(new Member(id, "Test Member", "test@example.com", "0700000000")), "Member insert failed");
            check(members.getAllMembers().stream().anyMatch(m -> id.equals(m.getMemberCode()) && m.isActiveStatus()), "Member defaults failed");
            Savepoint beforeInvalid = c.setSavepoint();
            check(!books.addBook(book), "Duplicate ISBN accepted");
            c.rollback(beforeInvalid);
            beforeInvalid = c.setSavepoint();
            check(!books.addBook(new Book(id + "-invalid", "Invalid", "Author", "Test", 2020, -1)), "Negative copies accepted");
            c.rollback(beforeInvalid);
            String input = "bad\n2\n2\nConsole " + id + "\nconsole@example.com\n0711111111\n3\n4\n0\n";
            InputStream originalIn = System.in;
            try {
                System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
                new LibraryService().startLibrarySystem();
            } finally { System.setIn(originalIn); }
            check(members.getAllMembers().stream().anyMatch(m -> ("Console " + id).equals(m.getName()) && "console@example.com".equals(m.getEmail())), "Console member input skipped or shifted");
            System.out.println("PASS: PostgreSQL CRUD, search, constraints, defaults and console input. Test rows rolled back.");
        } finally {
            c.rollback();
            DatabaseConnection.closeConnection();
        }
    }
}
