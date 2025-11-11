package library.main;

import library.service.LibraryService;

public class Main {
    public static void main(String[] args) {
        LibraryService libraryService = new LibraryService();
        libraryService.startLibrarySystem();
    }
}