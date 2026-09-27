package com.bookbridge.client.service;

import com.bookbridge.client.NetworkClient;
import com.bookbridge.model.Book;
import com.bookbridge.model.Branch;
import com.bookbridge.model.BorrowRecord;
import com.bookbridge.network.NetworkMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LibraryService {

    @SuppressWarnings("unchecked")
    public static List<Book> fetchAllBooks() {
        NetworkMessage req = new NetworkMessage("VIEW_ALL_BOOKS", null);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (res.success && res.responseData instanceof List) {
            return (List<Book>) res.responseData;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Book> searchBooks(String query, Integer branchId) {
        NetworkMessage req = new NetworkMessage("SEARCH_BOOK", new Object[]{query, branchId});
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (res.success && res.responseData instanceof List) {
            return (List<Book>) res.responseData;
        }
        return new ArrayList<>();
    }

    public static String borrowBook(int bookId, int userBranch) throws Exception {
        return borrowBook(bookId, userBranch, "Member");
    }

    public static String borrowBook(int bookId, int userBranch, String username) throws Exception {
        NetworkMessage req = new NetworkMessage("BORROW_BOOK", new Object[]{bookId, userBranch, username});
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (res.success) {
            return res.responseData != null ? res.responseData.toString() : "Success";
        }
        throw new Exception(res.errorMessage != null ? res.errorMessage : "Failed to borrow book.");
    }

    public static void returnBook(int bookId, int userBranch) throws Exception {
        returnBook(bookId, userBranch, null);
    }

    public static void returnBook(int bookId, int userBranch, String username) throws Exception {
        NetworkMessage req = new NetworkMessage("RETURN_BOOK", new Object[]{bookId, userBranch, username});
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (!res.success) {
            throw new Exception(res.errorMessage != null ? res.errorMessage : "Failed to return book.");
        }
    }

    @SuppressWarnings("unchecked")
    public static List<BorrowRecord> fetchUserBorrowedBooks(String username) {
        NetworkMessage req = new NetworkMessage("GET_USER_BORROWED_BOOKS", username);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (res.success && res.responseData instanceof List) {
            return (List<BorrowRecord>) res.responseData;
        }
        return new ArrayList<>();
    }

    public static void addBook(Book book) throws Exception {
        NetworkMessage req = new NetworkMessage("ADD_BOOK", book);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (!res.success) {
            throw new Exception(res.errorMessage != null ? res.errorMessage : "Failed to add book.");
        }
    }

    public static void deleteBook(int bookId) throws Exception {
        NetworkMessage req = new NetworkMessage("DELETE_BOOK", bookId);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (!res.success) {
            throw new Exception(res.errorMessage != null ? res.errorMessage : "Failed to delete book.");
        }
    }

    public static void updateBook(Book book) throws Exception {
        NetworkMessage req = new NetworkMessage("UPDATE_BOOK", book);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (!res.success) {
            throw new Exception(res.errorMessage != null ? res.errorMessage : "Failed to update book.");
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Branch> getBranches() {
        NetworkMessage req = new NetworkMessage("GET_BRANCHES", null);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (res.success && res.responseData instanceof List) {
            return (List<Branch>) res.responseData;
        }
        return new ArrayList<>();
    }

    public static String getBranchName(int branchId) {
        NetworkMessage req = new NetworkMessage("GET_BRANCH_NAME", branchId);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (res.success && res.responseData != null) {
            return (String) res.responseData;
        }
        return "Branch " + branchId;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getSystemStatistics() {
        NetworkMessage req = new NetworkMessage("GET_STATISTICS", null);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (res.success && res.responseData instanceof Map) {
            return (Map<String, Object>) res.responseData;
        }
        return Map.of();
    }

    // CLI console output helpers
    public static void viewAllBooks() {
        System.out.println("\n========== AVAILABLE BOOKS ==========\n");
        List<Book> books = fetchAllBooks();
        if (books.isEmpty()) {
            System.out.println("No books found in catalog.");
        } else {
            for (Book book : books) {
                System.out.println(book);
                System.out.println("--------------------------------------");
            }
        }
    }

    public static int searchBookCLI(String bookName, int userBranch) {
        List<Book> results = searchBooks(bookName, null);
        if (results.isEmpty()) {
            System.out.println("\n❌ Book not found in any branch.");
            System.out.println("💡 You can submit a Purchase Request for the library.");
            return 3;
        }

        Book localMatch = null;
        for (Book b : results) {
            if (b.getBranchId() == userBranch) {
                localMatch = b;
                break;
            }
        }

        if (localMatch != null) {
            System.out.println("\n✅ Book is available in your branch!");
            System.out.println(localMatch);
            return 1;
        } else {
            Book other = results.get(0);
            System.out.println("\n📍 Book is available at " + (other.getBranchName() != null ? other.getBranchName() : "Branch #" + other.getBranchId()) + ".");
            System.out.println("🔄 You can submit an Inter-Branch Transfer Request.");
            return 2;
        }
    }
}
