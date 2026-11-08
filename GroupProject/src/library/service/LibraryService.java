package library.service;

import library.dao.BookDAO;
import library.dao.MemberDAO;
import library.model.Book;
import library.model.Member;
import java.util.List;
import java.util.Scanner;

public class LibraryService {
    private final BookDAO bookDAO;
    private final MemberDAO memberDAO;
    private final Scanner scanner;

    public LibraryService() {
        this.bookDAO = new BookDAO();
        this.memberDAO = new MemberDAO();
        this.scanner = new Scanner(System.in);
    }

    public void startLibrarySystem() {
        System.out.println("🏛️  LIBRARY MANAGEMENT SYSTEM");
        System.out.println("================================");

        int choice;
        do {
            displayMainMenu();
            System.out.print("Enter your choice: ");
            choice = getIntInput();

            switch (choice) {
                case 1: manageBooks(); break;
                case 2: manageMembers(); break;
                case 3: searchBooks(); break;
                case 4: displayStatistics(); break;
                case 0: exitSystem(); break;
                default: System.out.println("❌ Invalid choice! Please try again.");
            }
        } while (choice != 0);
    }

    private void displayMainMenu() {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("MAIN MENU");
        System.out.println("=".repeat(40));
        System.out.println("1. Manage Books");
        System.out.println("2. Manage Members");
        System.out.println("3. Search Books");
        System.out.println("4. System Statistics");
        System.out.println("0. Exit System");
        System.out.println("-".repeat(40));
    }

    private void manageBooks() {
        int choice;
        do {
            System.out.println("\n--- BOOK MANAGEMENT ---");
            System.out.println("1. View All Books");
            System.out.println("2. Add New Book");
            System.out.println("3. Search Books");
            System.out.println("4. Back to Main Menu");
            System.out.print("Choose: ");
            choice = getIntInput();

            switch (choice) {
                case 1: displayAllBooks(); break;
                case 2: addNewBook(); break;
                case 3: searchBooks(); break;
                case 4: break;
                default: System.out.println("❌ Invalid choice!");
            }
        } while (choice != 4);
    }

    private void displayAllBooks() {
        List<Book> books = bookDAO.getAllBooks();
        System.out.println("\n=== ALL BOOKS ===");
        if (books.isEmpty()) {
            System.out.println("No books found in the library.");
        } else {
            for (int i = 0; i < books.size(); i++) {
                System.out.println("\n" + (i + 1) + ".");
                books.get(i).displayInfo();
            }
        }
    }

    private void addNewBook() {
        System.out.println("\n--- ADD NEW BOOK ---");

        System.out.print("Enter ISBN: ");
        String isbn = scanner.nextLine();

        System.out.print("Enter Title: ");
        String title = scanner.nextLine();

        System.out.print("Enter Author: ");
        String author = scanner.nextLine();

        System.out.print("Enter Genre: ");
        String genre = scanner.nextLine();

        System.out.print("Enter Published Year: ");
        int year = getIntInput();

        System.out.print("Enter Total Copies: ");
        int copies = getIntInput();

        if (isbn.trim().isEmpty() || title.trim().isEmpty() || author.trim().isEmpty()
                || year < 1 || year > 9999 || copies < 0) {
            System.out.println("Please provide ISBN, title, author, a year from 1 to 9999, and a non-negative copy count.");
            return;
        }
        Book newBook = new Book(isbn.trim(), title.trim(), author.trim(), genre.trim(), year, copies);

        if (bookDAO.addBook(newBook)) {
            System.out.println("✅ Book added successfully!");
        } else {
            System.out.println("❌ Failed to add book.");
        }
    }

    private void manageMembers() {
        int choice;
        do {
            System.out.println("\n--- MEMBER MANAGEMENT ---");
            System.out.println("1. View All Members");
            System.out.println("2. Add New Member");
            System.out.println("3. Back to Main Menu");
            System.out.print("Choose: ");
            choice = getIntInput();

            switch (choice) {
                case 1: displayAllMembers(); break;
                case 2: addNewMember(); break;
                case 3: break;
                default: System.out.println("❌ Invalid choice!");
            }
        } while (choice != 3);
    }

    private void displayAllMembers() {
        List<Member> members = memberDAO.getAllMembers();
        System.out.println("\n=== ALL MEMBERS ===");
        if (members.isEmpty()) {
            System.out.println("No members registered.");
        } else {
            for (int i = 0; i < members.size(); i++) {
                System.out.println("\n" + (i + 1) + ".");
                members.get(i).displayInfo();
            }
        }
    }

    private void addNewMember() {
        System.out.println("\n--- ADD NEW MEMBER ---");


        System.out.print("Enter Name: ");
        String name = scanner.nextLine();

        System.out.print("Enter Email: ");
        String email = scanner.nextLine();

        System.out.print("Enter Phone: ");
        String phone = scanner.nextLine();

        // Generate member code
        String memberCode = "MEM" + System.currentTimeMillis();

        if (name.trim().isEmpty()) {
            System.out.println("Member name cannot be blank.");
            return;
        }
        Member newMember = new Member(memberCode, name.trim(), email.trim(), phone.trim());

        if (memberDAO.addMember(newMember)) {
            System.out.println("✅ Member added successfully!");
            System.out.println("Member Code: " + memberCode);
        } else {
            System.out.println("❌ Failed to add member.");
        }
    }

    private void searchBooks() {
        System.out.print("\nEnter search term (title or author): ");
        String keyword = scanner.nextLine();

        List<Book> results = bookDAO.searchBooks(keyword);

        System.out.println("\n=== SEARCH RESULTS ===");
        if (results.isEmpty()) {
            System.out.println("No books found matching '" + keyword + "'");
        } else {
            for (int i = 0; i < results.size(); i++) {
                System.out.println("\n" + (i + 1) + ".");
                results.get(i).displayInfo();
            }
        }
    }

    private void displayStatistics() {
        List<Book> books = bookDAO.getAllBooks();
        List<Member> members = memberDAO.getAllMembers();

        System.out.println("\n=== SYSTEM STATISTICS ===");
        System.out.println("Total Books: " + books.size());
        System.out.println("Total Members: " + members.size());

        long availableBooks = books.stream().filter(Book::isAvailable).count();
        System.out.println("Available Books: " + availableBooks);
    }

    private void exitSystem() {
        System.out.println("\nThank you for using Library Management System! 👋");
        scanner.close();
    }

    private int getIntInput() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("❌ Please enter a valid number: ");
            }
        }
    }
}
