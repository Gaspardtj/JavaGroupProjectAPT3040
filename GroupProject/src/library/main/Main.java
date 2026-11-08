package library.main;

import library.service.LibraryService;
import library.dao.DatabaseConnection;
import java.util.NoSuchElementException;

public class Main {
    public static void main(String[] args) {
        int status = 0;
        try { new LibraryService().startLibrarySystem(); }
        catch (NoSuchElementException e) { System.out.println("\nInput closed. Goodbye!"); }
        catch (IllegalStateException e) { System.err.println(e.getMessage()); status = 1; }
        finally { DatabaseConnection.closeConnection(); }
        if (status != 0) System.exit(status);
    }
}
