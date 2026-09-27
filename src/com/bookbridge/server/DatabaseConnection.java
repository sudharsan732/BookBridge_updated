package com.bookbridge.server;

import com.bookbridge.model.Book;
import com.bookbridge.model.Branch;
import com.bookbridge.model.BorrowRecord;
import com.bookbridge.model.PurchaseRequest;
import com.bookbridge.model.TransferRequest;
import com.bookbridge.model.User;
import com.bookbridge.model.UserNotification;

import java.io.File;
import java.io.FileInputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseConnection {

    private static String dbHost = "localhost";
    private static int dbPort = 3306;
    private static String dbName = "bookbridge";
    private static String dbUser = "root";
    private static String dbPassword = "your_password";

    private static boolean useFallbackInMemory = false;
    private static boolean initialized = false;

    // --- IN-MEMORY FALLBACK DATA STORE (Thread-Safe) ---
    private static final Map<Integer, Branch> memoryBranches = new ConcurrentHashMap<>();
    private static final Map<Integer, Book> memoryBooks = new ConcurrentHashMap<>();
    private static final Map<Integer, User> memoryUsers = new ConcurrentHashMap<>();
    private static final List<TransferRequest> memoryTransferRequests = new CopyOnWriteArrayList<>();
    private static final List<PurchaseRequest> memoryPurchaseRequests = new CopyOnWriteArrayList<>();
    private static final List<BorrowRecord> memoryBorrowRecords = new CopyOnWriteArrayList<>();
    private static final List<UserNotification> memoryNotifications = new CopyOnWriteArrayList<>();
    private static final AtomicInteger transferIdGen = new AtomicInteger(100);
    private static final AtomicInteger purchaseIdGen = new AtomicInteger(100);
    private static final AtomicInteger userIdGen = new AtomicInteger(10);
    private static final AtomicInteger borrowIdGen = new AtomicInteger(100);
    private static final AtomicInteger bookIdGen = new AtomicInteger(500);
    private static final AtomicInteger notificationIdGen = new AtomicInteger(100);

    static {
        loadConfig();
        initDataSource();
    }

    private static void loadConfig() {
        Properties props = new Properties();
        File configFile = new File("config/db.properties");
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                dbHost = props.getProperty("db.host", dbHost);
                dbPort = Integer.parseInt(props.getProperty("db.port", String.valueOf(dbPort)));
                dbName = props.getProperty("db.name", dbName);
                dbUser = props.getProperty("db.user", dbUser);
                dbPassword = props.getProperty("db.password", dbPassword);
            } catch (Exception e) {
                System.err.println("[Database] Notice: Could not read config/db.properties, using defaults.");
            }
        }

        // Environment variable overrides
        if (System.getenv("DB_HOST") != null) dbHost = System.getenv("DB_HOST");
        if (System.getenv("DB_PORT") != null) {
            try { dbPort = Integer.parseInt(System.getenv("DB_PORT")); } catch (Exception ignored) {}
        }
        if (System.getenv("DB_NAME") != null) dbName = System.getenv("DB_NAME");
        if (System.getenv("DB_USER") != null) dbUser = System.getenv("DB_USER");
        if (System.getenv("DB_PASSWORD") != null) dbPassword = System.getenv("DB_PASSWORD");
    }

    private static synchronized void initDataSource() {
        if (initialized) return;

        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", dbHost, dbPort, dbName);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection testConn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
                useFallbackInMemory = false;
                System.out.println("✅ [Database] Successfully connected to MySQL at " + jdbcUrl);
                initMySqlSchema(testConn);
            }
        } catch (Exception e) {
            useFallbackInMemory = true;
            System.out.println("⚠️  [Database] MySQL unavailable (" + e.getMessage() + ").");
            System.out.println("🚀 [Database] Seamlessly initialized High-Speed In-Memory Data Store with seeded catalog & accounts.");
            seedInMemoryData();
        }
        initialized = true;
    }

    private static void initMySqlSchema(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS branches (branch_id INT PRIMARY KEY, branch_name VARCHAR(100), location VARCHAR(100))");
            stmt.execute("CREATE TABLE IF NOT EXISTS users (user_id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) UNIQUE NOT NULL, password VARCHAR(100) NOT NULL, full_name VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL DEFAULT 'MEMBER', branch_id INT, created_at VARCHAR(50))");
            stmt.execute("CREATE TABLE IF NOT EXISTS books (book_id INT PRIMARY KEY, title VARCHAR(100), author VARCHAR(100), available_copies INT, branch_id INT, category VARCHAR(100) DEFAULT 'Computer Science')");
            stmt.execute("CREATE TABLE IF NOT EXISTS transfer_requests (id INT AUTO_INCREMENT PRIMARY KEY, book_name VARCHAR(100), from_branch VARCHAR(100), to_branch VARCHAR(100), requester_name VARCHAR(100) DEFAULT 'Member', quantity INT DEFAULT 1, status VARCHAR(50) DEFAULT 'PENDING', request_date VARCHAR(50))");
            stmt.execute("CREATE TABLE IF NOT EXISTS purchase_requests (id INT AUTO_INCREMENT PRIMARY KEY, book_name VARCHAR(100), author VARCHAR(100) DEFAULT 'Unknown', category VARCHAR(100) DEFAULT 'General', requested_branch VARCHAR(100) DEFAULT 'Guindy Library', requester_name VARCHAR(100) DEFAULT 'Member', status VARCHAR(50) DEFAULT 'PENDING', request_date VARCHAR(50))");
            stmt.execute("CREATE TABLE IF NOT EXISTS borrow_records (id INT AUTO_INCREMENT PRIMARY KEY, user_id INT, username VARCHAR(50) NOT NULL, book_id INT NOT NULL, book_title VARCHAR(100) NOT NULL, branch_id INT NOT NULL, borrow_date VARCHAR(50), return_date VARCHAR(50), status VARCHAR(20) DEFAULT 'BORROWED')");
            stmt.execute("CREATE TABLE IF NOT EXISTS user_notifications (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL, message VARCHAR(255) NOT NULL, type VARCHAR(20) DEFAULT 'INFO', created_at VARCHAR(50), is_read BOOLEAN DEFAULT FALSE)");

            // Seed branches if empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM branches");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO branches VALUES (1, 'Guindy Library', 'Guindy'), (2, 'Adyar Library', 'Adyar'), (3, 'Velachery Library', 'Velachery')");
                
                // Seed Users
                stmt.executeUpdate("INSERT INTO users (user_id, username, password, full_name, role, branch_id, created_at) VALUES " +
                        "(1, 'admin', 'admin123', 'System Administrator', 'ADMIN', 1, '2026-09-09 10:00'), " +
                        "(2, 'purushothaman', 'user123', 'Purushothaman', 'MEMBER', 1, '2026-09-09 10:00'), " +
                        "(3, 'alice', 'user123', 'Alice Johnson', 'MEMBER', 2, '2026-09-09 10:00')");

                // Seed Books
                stmt.executeUpdate("INSERT INTO books (book_id, title, author, available_copies, branch_id, category) VALUES " +
                        "(101, 'Clean Code', 'Robert C. Martin', 5, 1, 'Software Engineering'), " +
                        "(102, 'Java: The Complete Reference', 'Herbert Schildt', 3, 1, 'Programming'), " +
                        "(103, 'Data Structures & Algorithms', 'Mark Allen Weiss', 4, 2, 'Computer Science'), " +
                        "(104, 'Operating System Concepts', 'Silberschatz & Galvin', 2, 2, 'Systems'), " +
                        "(105, 'Computer Networks', 'Andrew S. Tanenbaum', 6, 3, 'Networking'), " +
                        "(106, 'Database System Concepts', 'Korth & Sudarshan', 1, 3, 'Databases'), " +
                        "(107, 'Design Patterns (GoF)', 'Erich Gamma et al.', 4, 1, 'Architecture'), " +
                        "(108, 'Designing Data-Intensive Applications', 'Martin Kleppmann', 3, 2, 'Distributed Systems')");
            }

            try (ResultSet rsMax = stmt.executeQuery("SELECT MAX(book_id) FROM books")) {
                if (rsMax.next()) {
                    int maxId = rsMax.getInt(1);
                    if (maxId > bookIdGen.get()) {
                        bookIdGen.set(maxId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database] Schema init notice: " + e.getMessage());
        }
    }

    private static void seedInMemoryData() {
        memoryBranches.put(1, new Branch(1, "Guindy Library", "Guindy"));
        memoryBranches.put(2, new Branch(2, "Adyar Library", "Adyar"));
        memoryBranches.put(3, new Branch(3, "Velachery Library", "Velachery"));

        // Seed Users
        User admin = new User(1, "admin", "admin123", "System Administrator", "ADMIN", 1, "Guindy Library", "2026-09-09 10:00");
        User user1 = new User(2, "purushothaman", "user123", "Purushothaman", "MEMBER", 1, "Guindy Library", "2026-09-09 10:00");
        User user2 = new User(3, "alice", "user123", "Alice Johnson", "MEMBER", 2, "Adyar Library", "2026-09-09 10:00");

        memoryUsers.put(admin.getUserId(), admin);
        memoryUsers.put(user1.getUserId(), user1);
        memoryUsers.put(user2.getUserId(), user2);

        // Seed Books
        Book[] seedBooks = new Book[] {
            new Book(101, "Clean Code", "Robert C. Martin", 5, 1, "Software Engineering"),
            new Book(102, "Java: The Complete Reference", "Herbert Schildt", 3, 1, "Programming"),
            new Book(103, "Data Structures & Algorithms", "Mark Allen Weiss", 4, 2, "Computer Science"),
            new Book(104, "Operating System Concepts", "Silberschatz & Galvin", 2, 2, "Systems"),
            new Book(105, "Computer Networks", "Andrew S. Tanenbaum", 6, 3, "Networking"),
            new Book(106, "Database System Concepts", "Korth & Sudarshan", 1, 3, "Databases"),
            new Book(107, "Design Patterns (GoF)", "Erich Gamma et al.", 4, 1, "Architecture"),
            new Book(108, "Designing Data-Intensive Applications", "Martin Kleppmann", 3, 2, "Distributed Systems")
        };

        for (Book b : seedBooks) {
            Branch br = memoryBranches.get(b.getBranchId());
            if (br != null) b.setBranchName(br.getBranchName());
            memoryBooks.put(b.getBookId(), b);
        }

        memoryTransferRequests.add(new TransferRequest(transferIdGen.incrementAndGet(), "Clean Code", "Guindy Library", "Adyar Library", "Alice Johnson", 1, "PENDING", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())));
        memoryPurchaseRequests.add(new PurchaseRequest(purchaseIdGen.incrementAndGet(), "Refactoring: Improving the Design of Existing Code", "Martin Fowler", "Software Engineering", "Guindy Library", "Purushothaman", "PENDING", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())));
    }

    public static Connection getConnection() throws SQLException {
        if (useFallbackInMemory) return null;
        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", dbHost, dbPort, dbName);
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
    }

    // =========================================================================
    // AUTHENTICATION & USER MANAGEMENT
    // =========================================================================

    public static User authenticate(String username, String password) throws Exception {
        if (username == null || password == null) {
            throw new Exception("Username and password are required.");
        }
        String u = username.trim();
        String p = password.trim();

        if (useFallbackInMemory) {
            for (User user : memoryUsers.values()) {
                if (user.getUsername().equalsIgnoreCase(u) && user.getPassword().equals(p)) {
                    Branch br = memoryBranches.get(user.getBranchId());
                    user.setBranchName(br != null ? br.getBranchName() : "Branch " + user.getBranchId());
                    return user;
                }
            }

            if (u.equalsIgnoreCase("admin") && p.equals("admin123")) {
                User admin = new User(1, "admin", "admin123", "System Administrator", "ADMIN", 1, "Guindy Library", "2026-09-09 10:00");
                memoryUsers.put(1, admin);
                return admin;
            }
            if (u.equalsIgnoreCase("purushothaman") && p.equals("user123")) {
                User user1 = new User(2, "purushothaman", "user123", "Purushothaman", "MEMBER", 1, "Guindy Library", "2026-09-09 10:00");
                memoryUsers.put(2, user1);
                return user1;
            }
            if (u.equalsIgnoreCase("alice") && p.equals("user123")) {
                User user2 = new User(3, "alice", "user123", "Alice Johnson", "MEMBER", 2, "Adyar Library", "2026-09-09 10:00");
                memoryUsers.put(3, user2);
                return user2;
            }
            throw new Exception("Invalid username or password.");
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT u.*, b.branch_name FROM users u LEFT JOIN branches b ON u.branch_id = b.branch_id WHERE LOWER(u.username) = LOWER(?) AND u.password = ?")) {
            ps.setString(1, u);
            ps.setString(2, p);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getInt("branch_id"),
                        rs.getString("branch_name"),
                        rs.getString("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            throw new Exception("Database error during authentication: " + e.getMessage());
        }
        throw new Exception("Invalid username or password.");
    }

    public static synchronized void createUser(User user) throws Exception {
        if (user == null || user.getUsername() == null || user.getPassword() == null) {
            throw new Exception("User details are incomplete.");
        }
        String username = user.getUsername().trim().toLowerCase();

        if (useFallbackInMemory) {
            for (User u : memoryUsers.values()) {
                if (u.getUsername().equalsIgnoreCase(username)) {
                    throw new Exception("Username '" + user.getUsername() + "' is already registered.");
                }
            }
            user.setUserId(userIdGen.incrementAndGet());
            Branch br = memoryBranches.get(user.getBranchId());
            if (br != null) user.setBranchName(br.getBranchName());
            memoryUsers.put(user.getUserId(), user);
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO users (username, password, full_name, role, branch_id, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole() != null ? user.getRole() : "MEMBER");
            ps.setInt(5, user.getBranchId());
            ps.setString(6, user.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage().toLowerCase().contains("duplicate") || e.getMessage().toLowerCase().contains("unique")) {
                throw new Exception("Username '" + user.getUsername() + "' is already registered.");
            }
            throw new Exception("Database error creating user: " + e.getMessage());
        }
    }

    public static List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        if (useFallbackInMemory) {
            for (User u : memoryUsers.values()) {
                Branch br = memoryBranches.get(u.getBranchId());
                u.setBranchName(br != null ? br.getBranchName() : "Branch " + u.getBranchId());
                list.add(u);
            }
            list.sort(Comparator.comparingInt(User::getUserId));
            return list;
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT u.*, b.branch_name FROM users u LEFT JOIN branches b ON u.branch_id = b.branch_id ORDER BY u.user_id ASC")) {
            while (rs.next()) {
                list.add(new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    "***",
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getInt("branch_id"),
                    rs.getString("branch_name"),
                    rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] getAllUsers: " + e.getMessage());
        }
        return list;
    }

    public static synchronized void deleteUser(int userId) throws Exception {
        if (userId == 1) {
            throw new Exception("Cannot delete the root System Administrator.");
        }

        if (useFallbackInMemory) {
            if (memoryUsers.remove(userId) == null) {
                throw new Exception("User ID #" + userId + " not found.");
            }
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            int count = ps.executeUpdate();
            if (count == 0) throw new Exception("User ID #" + userId + " not found.");
        }
    }

    // =========================================================================
    // BOOK & CATALOG OPERATIONS (Branch-Wise Inventory)
    // =========================================================================

    public static List<Book> getAllBooks() {
        List<Book> list = new ArrayList<>();
        if (useFallbackInMemory) {
            for (Book b : memoryBooks.values()) {
                Branch br = memoryBranches.get(b.getBranchId());
                b.setBranchName(br != null ? br.getBranchName() : "Branch " + b.getBranchId());
                list.add(b);
            }
            list.sort(Comparator.comparingInt(Book::getBookId));
            return list;
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT b.*, br.branch_name FROM books b LEFT JOIN branches br ON b.branch_id = br.branch_id ORDER BY b.book_id ASC")) {
            while (rs.next()) {
                Book book = new Book(
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getInt("available_copies"),
                    rs.getInt("branch_id"),
                    rs.getString("category")
                );
                book.setBranchName(rs.getString("branch_name"));
                list.add(book);
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] getAllBooks: " + e.getMessage());
        }
        return list;
    }

    public static List<Book> searchBooks(String query, Integer branchId) {
        List<Book> all = getAllBooks();
        List<Book> filtered = new ArrayList<>();
        String q = (query != null) ? query.trim().toLowerCase() : "";

        for (Book b : all) {
            boolean matchesQuery = q.isEmpty() ||
                    b.getTitle().toLowerCase().contains(q) ||
                    b.getAuthor().toLowerCase().contains(q) ||
                    (b.getCategory() != null && b.getCategory().toLowerCase().contains(q)) ||
                    String.valueOf(b.getBookId()).equals(q);

            boolean matchesBranch = (branchId == null || branchId <= 0 || b.getBranchId() == branchId);

            if (matchesQuery && matchesBranch) {
                filtered.add(b);
            }
        }
        return filtered;
    }

    public static Book getBookById(int bookId) {
        if (useFallbackInMemory) {
            return memoryBooks.get(bookId);
        }
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT b.*, br.branch_name FROM books b LEFT JOIN branches br ON b.branch_id = br.branch_id WHERE b.book_id = ?")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Book b = new Book(
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("available_copies"),
                        rs.getInt("branch_id"),
                        rs.getString("category")
                    );
                    b.setBranchName(rs.getString("branch_name"));
                    return b;
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] getBookById: " + e.getMessage());
        }
        return null;
    }

    public static synchronized String borrowBook(int bookId, int userBranchId) throws Exception {
        return borrowBook(bookId, userBranchId, "Member");
    }

    public static synchronized String borrowBook(int bookId, int userBranchId, String username) throws Exception {
        String borrower = (username != null && !username.trim().isEmpty()) ? username.trim() : "Member";
        System.out.println("[DEBUG borrowBook] username='" + username + "' borrower='" + borrower + "' bookId=" + bookId + " branch=" + userBranchId);

        if (useFallbackInMemory) {
            Book book = memoryBooks.get(bookId);
            if (book == null) throw new Exception("Book ID #" + bookId + " does not exist.");
            if (book.getBranchId() != userBranchId) {
                Branch targetBranch = memoryBranches.get(book.getBranchId());
                String targetName = targetBranch != null ? targetBranch.getBranchName() : "Branch " + book.getBranchId();
                throw new Exception("Book is located at '" + targetName + "', not your home branch.");
            }
            if (book.getAvailableCopies() <= 0) {
                throw new Exception("No available copies of '" + book.getTitle() + "' at your branch.");
            }

            // Check duplicate active borrow
            for (BorrowRecord rec : memoryBorrowRecords) {
                if ("BORROWED".equalsIgnoreCase(rec.getStatus()) && rec.getBookId() == bookId && rec.getUsername().equalsIgnoreCase(borrower)) {
                    throw new Exception("You have already borrowed '" + book.getTitle() + "'. Please return your current copy first.");
                }
            }

            book.setAvailableCopies(book.getAvailableCopies() - 1);
            BorrowRecord br = new BorrowRecord(
                borrowIdGen.incrementAndGet(),
                0,
                borrower,
                bookId,
                book.getTitle(),
                userBranchId,
                book.getBranchName() != null ? book.getBranchName() : getBranchName(userBranchId),
                new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()),
                null,
                "BORROWED"
            );
            memoryBorrowRecords.add(0, br);
            return "Remaining Copies: " + book.getAvailableCopies();
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement checkStmt = conn.prepareStatement("SELECT available_copies, branch_id, title FROM books WHERE book_id = ? FOR UPDATE");
                checkStmt.setInt(1, bookId);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    conn.rollback();
                    throw new Exception("Book ID #" + bookId + " not found.");
                }

                int copies = rs.getInt("available_copies");
                int branch = rs.getInt("branch_id");
                String title = rs.getString("title");

                if (branch != userBranchId) {
                    conn.rollback();
                    throw new Exception("Book is available at Branch #" + branch + ", not your home branch.");
                }
                if (copies <= 0) {
                    conn.rollback();
                    throw new Exception("No available copies of '" + title + "' at your branch.");
                }

                // Check duplicate borrow
                PreparedStatement dupCheck = conn.prepareStatement("SELECT id FROM borrow_records WHERE book_id = ? AND username = ? AND status = 'BORROWED'");
                dupCheck.setInt(1, bookId);
                dupCheck.setString(2, borrower);
                if (dupCheck.executeQuery().next()) {
                    conn.rollback();
                    throw new Exception("You have already borrowed '" + title + "'. Please return your copy first.");
                }

                PreparedStatement updateStmt = conn.prepareStatement("UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ?");
                updateStmt.setInt(1, bookId);
                updateStmt.executeUpdate();

                PreparedStatement recordStmt = conn.prepareStatement("INSERT INTO borrow_records (username, book_id, book_title, branch_id, borrow_date, status) VALUES (?, ?, ?, ?, ?, 'BORROWED')");
                recordStmt.setString(1, borrower);
                recordStmt.setInt(2, bookId);
                recordStmt.setString(3, title);
                recordStmt.setInt(4, userBranchId);
                recordStmt.setString(5, new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                recordStmt.executeUpdate();

                conn.commit();
                return "Remaining Copies: " + (copies - 1);
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    public static synchronized void returnBook(int bookId, int userBranchId) throws Exception {
        returnBook(bookId, userBranchId, null);
    }

    public static synchronized void returnBook(int bookId, int userBranchId, String username) throws Exception {
        if (useFallbackInMemory) {
            Book book = memoryBooks.get(bookId);
            if (book == null) throw new Exception("Book ID #" + bookId + " does not exist.");

            BorrowRecord recordToReturn = null;
            for (BorrowRecord rec : memoryBorrowRecords) {
                if ("BORROWED".equalsIgnoreCase(rec.getStatus()) && rec.getBookId() == bookId) {
                    if (username == null || username.trim().isEmpty() || rec.getUsername().equalsIgnoreCase(username.trim())) {
                        recordToReturn = rec;
                        break;
                    }
                }
            }

            if (recordToReturn == null) {
                throw new Exception("No active borrowed record found for Book ID #" + bookId + ".");
            }

            recordToReturn.setStatus("RETURNED");
            recordToReturn.setReturnDate(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            return;
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "SELECT id FROM borrow_records WHERE book_id = ? AND status = 'BORROWED'" + (username != null ? " AND username = ?" : "") + " LIMIT 1";
                PreparedStatement findStmt = conn.prepareStatement(sql);
                findStmt.setInt(1, bookId);
                if (username != null) findStmt.setString(2, username);
                ResultSet rs = findStmt.executeQuery();
                if (!rs.next()) {
                    conn.rollback();
                    throw new Exception("No active borrowed record found for Book ID #" + bookId + ".");
                }
                int recordId = rs.getInt("id");

                PreparedStatement updateRecord = conn.prepareStatement("UPDATE borrow_records SET status = 'RETURNED', return_date = ? WHERE id = ?");
                updateRecord.setString(1, new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                updateRecord.setInt(2, recordId);
                updateRecord.executeUpdate();

                PreparedStatement retStmt = conn.prepareStatement("UPDATE books SET available_copies = available_copies + 1 WHERE book_id = ?");
                retStmt.setInt(1, bookId);
                retStmt.executeUpdate();

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    public static List<BorrowRecord> getUserBorrowedBooks(String username) {
        List<BorrowRecord> list = new ArrayList<>();
        if (username == null || username.trim().isEmpty()) return list;
        String u = username.trim();

        if (useFallbackInMemory) {
            for (BorrowRecord br : memoryBorrowRecords) {
                if ("BORROWED".equalsIgnoreCase(br.getStatus()) && (br.getUsername().equalsIgnoreCase(u))) {
                    list.add(br);
                }
            }
            return list;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT br.*, b.branch_name FROM borrow_records br LEFT JOIN branches b ON br.branch_id = b.branch_id WHERE LOWER(br.username) = LOWER(?) AND br.status = 'BORROWED' ORDER BY br.id DESC")) {
            ps.setString(1, u);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BorrowRecord(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getInt("book_id"),
                        rs.getString("book_title"),
                        rs.getInt("branch_id"),
                        rs.getString("branch_name"),
                        rs.getString("borrow_date"),
                        rs.getString("return_date"),
                        rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] getUserBorrowedBooks: " + e.getMessage());
        }
        return list;
    }

    public static synchronized void addBook(Book book) throws Exception {
        if (useFallbackInMemory) {
            // Check if book exists at same branch
            for (Book b : memoryBooks.values()) {
                if (b.getBookId() == book.getBookId()) {
                    throw new Exception("Book with ID #" + book.getBookId() + " already exists.");
                }
            }
            Branch br = memoryBranches.get(book.getBranchId());
            if (br != null) book.setBranchName(br.getBranchName());
            memoryBooks.put(book.getBookId(), book);
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO books (book_id, title, author, available_copies, branch_id, category) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, book.getBookId());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setInt(4, book.getAvailableCopies());
            ps.setInt(5, book.getBranchId());
            ps.setString(6, book.getCategory());
            ps.executeUpdate();
        }
    }

    public static synchronized void deleteBook(int bookId) throws Exception {
        if (useFallbackInMemory) {
            if (memoryBooks.remove(bookId) == null) {
                throw new Exception("Book ID #" + bookId + " not found.");
            }
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE book_id = ?")) {
            ps.setInt(1, bookId);
            int count = ps.executeUpdate();
            if (count == 0) throw new Exception("Book ID #" + bookId + " not found.");
        }
    }

    public static synchronized void updateBook(Book book) throws Exception {
        if (useFallbackInMemory) {
            if (!memoryBooks.containsKey(book.getBookId())) {
                throw new Exception("Book ID #" + book.getBookId() + " not found.");
            }
            Branch br = memoryBranches.get(book.getBranchId());
            if (br != null) book.setBranchName(br.getBranchName());
            memoryBooks.put(book.getBookId(), book);
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE books SET title = ?, author = ?, available_copies = ?, branch_id = ?, category = ? WHERE book_id = ?")) {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setInt(3, book.getAvailableCopies());
            ps.setInt(4, book.getBranchId());
            ps.setString(5, book.getCategory());
            ps.setInt(6, book.getBookId());
            ps.executeUpdate();
        }
    }

    public static List<Branch> getBranches() {
        List<Branch> list = new ArrayList<>();
        if (useFallbackInMemory) {
            list.addAll(memoryBranches.values());
            list.sort(Comparator.comparingInt(Branch::getBranchId));
            return list;
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM branches ORDER BY branch_id ASC")) {
            while (rs.next()) {
                list.add(new Branch(rs.getInt("branch_id"), rs.getString("branch_name"), rs.getString("location")));
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] getBranches: " + e.getMessage());
        }
        return list;
    }

    public static String getBranchName(int branchId) {
        if (useFallbackInMemory) {
            Branch b = memoryBranches.get(branchId);
            return b != null ? b.getBranchName() : "Branch " + branchId;
        }
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT branch_name FROM branches WHERE branch_id = ?")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("branch_name");
            }
        } catch (SQLException ignored) {}
        return "Branch " + branchId;
    }

    public static int resolveBranchId(String branchNameOrId) {
        if (branchNameOrId == null) return 1;
        String name = branchNameOrId.trim();
        for (Branch b : getBranches()) {
            if (b.getBranchName().equalsIgnoreCase(name) || String.valueOf(b.getBranchId()).equals(name) || b.getLocation().equalsIgnoreCase(name)) {
                return b.getBranchId();
            }
        }
        return 1;
    }

    public static synchronized void addNotification(UserNotification note) {
        if (note == null || note.getUsername() == null || note.getMessage() == null) return;
        if (useFallbackInMemory) {
            note.setId(notificationIdGen.incrementAndGet());
            memoryNotifications.add(0, note);
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO user_notifications (username, message, type, created_at, is_read) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, note.getUsername());
            ps.setString(2, note.getMessage());
            ps.setString(3, note.getType());
            ps.setString(4, note.getCreatedAt());
            ps.setBoolean(5, note.isRead());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Database Error] addNotification: " + e.getMessage());
        }
    }

    public static List<UserNotification> getUserNotifications(String username) {
        List<UserNotification> list = new ArrayList<>();
        if (username == null || username.trim().isEmpty()) return list;
        String u = username.trim();

        if (useFallbackInMemory) {
            for (UserNotification n : memoryNotifications) {
                if (n.getUsername().equalsIgnoreCase(u)) {
                    list.add(n);
                }
            }
            return list;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM user_notifications WHERE LOWER(username) = LOWER(?) ORDER BY id DESC")) {
            ps.setString(1, u);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new UserNotification(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("message"),
                        rs.getString("type"),
                        rs.getString("created_at"),
                        rs.getBoolean("is_read")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] getUserNotifications: " + e.getMessage());
        }
        return list;
    }

    public static synchronized void addTransferRequest(TransferRequest tr) throws Exception {
        if (tr == null || tr.getBookName() == null || tr.getRequesterName() == null) {
            throw new Exception("Transfer request details are incomplete.");
        }

        // Duplicate PENDING request check
        for (TransferRequest existing : getTransferRequests()) {
            if ("PENDING".equalsIgnoreCase(existing.getStatus()) &&
                tr.getRequesterName().equalsIgnoreCase(existing.getRequesterName()) &&
                tr.getBookName().equalsIgnoreCase(existing.getBookName()) &&
                tr.getToBranch().equalsIgnoreCase(existing.getToBranch())) {
                throw new Exception("You already have a pending transfer request for '" + tr.getBookName() + "'.");
            }
        }

        if (useFallbackInMemory) {
            tr.setId(transferIdGen.incrementAndGet());
            memoryTransferRequests.add(0, tr);
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO transfer_requests (book_name, from_branch, to_branch, requester_name, quantity, status, request_date) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, tr.getBookName());
            ps.setString(2, tr.getFromBranch());
            ps.setString(3, tr.getToBranch());
            ps.setString(4, tr.getRequesterName());
            ps.setInt(5, tr.getQuantity() > 0 ? tr.getQuantity() : 1);
            ps.setString(6, tr.getStatus());
            ps.setString(7, tr.getRequestDate());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Database Error] addTransferRequest: " + e.getMessage());
        }
    }

    public static List<TransferRequest> getTransferRequests() {
        if (useFallbackInMemory) {
            return new ArrayList<>(memoryTransferRequests);
        }

        List<TransferRequest> list = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM transfer_requests ORDER BY id DESC")) {
            while (rs.next()) {
                list.add(new TransferRequest(
                    rs.getInt("id"),
                    rs.getString("book_name"),
                    rs.getString("from_branch"),
                    rs.getString("to_branch"),
                    rs.getString("requester_name"),
                    rs.getInt("quantity") > 0 ? rs.getInt("quantity") : 1,
                    rs.getString("status"),
                    rs.getString("request_date")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] getTransferRequests: " + e.getMessage());
        }
        return list;
    }

    public static List<TransferRequest> getUserTransferRequests(String username) {
        List<TransferRequest> all = getTransferRequests();
        List<TransferRequest> userList = new ArrayList<>();
        if (username == null) return userList;
        for (TransferRequest tr : all) {
            if (username.equalsIgnoreCase(tr.getRequesterName())) {
                userList.add(tr);
            }
        }
        return userList;
    }

    public static synchronized void updateTransferStatus(int id, String status) throws Exception {
        System.out.println("[DEBUG updateTransferStatus] id=" + id + " newStatus=" + status);
        TransferRequest targetRequest = null;
        for (TransferRequest tr : getTransferRequests()) {
            if (tr.getId() == id) {
                targetRequest = tr;
                break;
            }
        }

        if (targetRequest == null) {
            throw new Exception("Transfer Request #" + id + " not found.");
        }

        if ("APPROVED".equalsIgnoreCase(status)) {
            if (!"PENDING".equalsIgnoreCase(targetRequest.getStatus())) {
                throw new Exception("Transfer Request #" + id + " has already been processed (Status: " + targetRequest.getStatus() + ").");
            }

            int fromBranchId = resolveBranchId(targetRequest.getFromBranch());
            int toBranchId = resolveBranchId(targetRequest.getToBranch());
            int qty = targetRequest.getQuantity() > 0 ? targetRequest.getQuantity() : 1;

            List<Book> books = getAllBooks();
            Book sourceBook = null;
            for (Book b : books) {
                if (b.getTitle().equalsIgnoreCase(targetRequest.getBookName()) && b.getBranchId() == fromBranchId) {
                    sourceBook = b;
                    break;
                }
            }

            if (sourceBook == null || sourceBook.getAvailableCopies() < qty) {
                int available = sourceBook != null ? sourceBook.getAvailableCopies() : 0;
                throw new Exception("Source branch '" + targetRequest.getFromBranch() + "' has insufficient inventory (" + available + " available, " + qty + " required).");
            }

            // Perform inventory transfer
            sourceBook.setAvailableCopies(sourceBook.getAvailableCopies() - qty);
            updateBook(sourceBook);

            Book destBook = null;
            for (Book b : books) {
                if (b.getTitle().equalsIgnoreCase(targetRequest.getBookName()) && b.getBranchId() == toBranchId) {
                    destBook = b;
                    break;
                }
            }

            if (destBook != null) {
                destBook.setAvailableCopies(destBook.getAvailableCopies() + qty);
                updateBook(destBook);
            } else {
                int newId = bookIdGen.incrementAndGet();
                Book newDestBook = new Book(
                    newId,
                    sourceBook.getTitle(),
                    sourceBook.getAuthor(),
                    qty,
                    toBranchId,
                    sourceBook.getCategory()
                );
                addBook(newDestBook);
            }

            targetRequest.setStatus("APPROVED");
            addNotification(new UserNotification(0, targetRequest.getRequesterName(), "Transfer approved: '" + targetRequest.getBookName() + "' is now available at " + targetRequest.getToBranch() + ".", "SUCCESS"));
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            targetRequest.setStatus("REJECTED");
            addNotification(new UserNotification(0, targetRequest.getRequesterName(), "Transfer request rejected for '" + targetRequest.getBookName() + "'.", "ERROR"));
        } else {
            targetRequest.setStatus(status);
        }

        if (!useFallbackInMemory) {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE transfer_requests SET status = ? WHERE id = ?")) {
                ps.setString(1, targetRequest.getStatus());
                ps.setInt(2, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Database Error] updateTransferStatus DB sync: " + e.getMessage());
            }
        }
    }

    public static synchronized void addPurchaseRequest(PurchaseRequest pr) throws Exception {
        if (pr == null || pr.getBookName() == null || pr.getRequesterName() == null) {
            throw new Exception("Purchase request details are incomplete.");
        }

        // Duplicate PENDING purchase check
        for (PurchaseRequest existing : getPurchaseRequests()) {
            if ("PENDING".equalsIgnoreCase(existing.getStatus()) &&
                pr.getRequesterName().equalsIgnoreCase(existing.getRequesterName()) &&
                pr.getBookName().equalsIgnoreCase(existing.getBookName())) {
                throw new Exception("You already have a pending purchase request for '" + pr.getBookName() + "'.");
            }
        }

        if (useFallbackInMemory) {
            pr.setId(purchaseIdGen.incrementAndGet());
            memoryPurchaseRequests.add(0, pr);
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO purchase_requests (book_name, author, category, requested_branch, requester_name, status, request_date) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, pr.getBookName());
            ps.setString(2, pr.getAuthor());
            ps.setString(3, pr.getCategory());
            ps.setString(4, pr.getRequestedBranch());
            ps.setString(5, pr.getRequesterName());
            ps.setString(6, pr.getStatus());
            ps.setString(7, pr.getRequestDate());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Database Error] addPurchaseRequest: " + e.getMessage());
        }
    }

    public static List<PurchaseRequest> getPurchaseRequests() {
        if (useFallbackInMemory) {
            return new ArrayList<>(memoryPurchaseRequests);
        }

        List<PurchaseRequest> list = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM purchase_requests ORDER BY id DESC")) {
            while (rs.next()) {
                list.add(new PurchaseRequest(
                    rs.getInt("id"),
                    rs.getString("book_name"),
                    rs.getString("author"),
                    rs.getString("category") != null ? rs.getString("category") : "General",
                    rs.getString("requested_branch") != null ? rs.getString("requested_branch") : "Guindy Library",
                    rs.getString("requester_name"),
                    rs.getString("status"),
                    rs.getString("request_date")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] getPurchaseRequests: " + e.getMessage());
        }
        return list;
    }

    public static List<PurchaseRequest> getUserPurchaseRequests(String username) {
        List<PurchaseRequest> all = getPurchaseRequests();
        List<PurchaseRequest> userList = new ArrayList<>();
        if (username == null) return userList;
        for (PurchaseRequest pr : all) {
            if (username.equalsIgnoreCase(pr.getRequesterName())) {
                userList.add(pr);
            }
        }
        return userList;
    }

    public static synchronized void updatePurchaseStatus(int id, String status) throws Exception {
        PurchaseRequest targetRequest = null;
        for (PurchaseRequest pr : getPurchaseRequests()) {
            if (pr.getId() == id) {
                targetRequest = pr;
                break;
            }
        }

        if (targetRequest == null) {
            throw new Exception("Purchase Request #" + id + " not found.");
        }

        if ("RECEIVED".equalsIgnoreCase(status)) {
            if ("RECEIVED".equalsIgnoreCase(targetRequest.getStatus())) {
                throw new Exception("Purchase Request #" + id + " has already been marked as RECEIVED.");
            }

            int reqBranchId = resolveBranchId(targetRequest.getRequestedBranch());
            List<Book> books = getAllBooks();
            Book targetBook = null;
            for (Book b : books) {
                if (b.getTitle().equalsIgnoreCase(targetRequest.getBookName()) && b.getBranchId() == reqBranchId) {
                    targetBook = b;
                    break;
                }
            }

            if (targetBook != null) {
                targetBook.setAvailableCopies(targetBook.getAvailableCopies() + 1);
                updateBook(targetBook);
            } else {
                int newId = bookIdGen.incrementAndGet();
                Book newBook = new Book(
                    newId,
                    targetRequest.getBookName(),
                    targetRequest.getAuthor(),
                    1,
                    reqBranchId,
                    targetRequest.getCategory()
                );
                addBook(newBook);
            }

            targetRequest.setStatus("RECEIVED");
            addNotification(new UserNotification(0, targetRequest.getRequesterName(), "'" + targetRequest.getBookName() + "' has been received at " + targetRequest.getRequestedBranch() + ". You can now borrow it.", "SUCCESS"));
        } else if ("APPROVED".equalsIgnoreCase(status)) {
            targetRequest.setStatus("APPROVED");
            addNotification(new UserNotification(0, targetRequest.getRequesterName(), "Purchase request approved for '" + targetRequest.getBookName() + "'. The book will become available after it is received.", "INFO"));
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            targetRequest.setStatus("REJECTED");
            addNotification(new UserNotification(0, targetRequest.getRequesterName(), "Your purchase request for '" + targetRequest.getBookName() + "' was rejected.", "ERROR"));
        } else {
            targetRequest.setStatus(status);
        }

        if (!useFallbackInMemory) {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE purchase_requests SET status = ? WHERE id = ?")) {
                ps.setString(1, targetRequest.getStatus());
                ps.setInt(2, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Database Error] updatePurchaseStatus DB sync: " + e.getMessage());
            }
        }
    }

    public static Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<Book> books = getAllBooks();
        int totalTitles = books.size();
        int totalCopies = 0;
        for (Book b : books) totalCopies += b.getAvailableCopies();

        int totalBranches = getBranches().size();
        int totalUsers = getAllUsers().size();
        int pendingTransfers = 0;
        for (TransferRequest tr : getTransferRequests()) {
            if ("PENDING".equalsIgnoreCase(tr.getStatus())) pendingTransfers++;
        }
        int pendingPurchases = 0;
        for (PurchaseRequest pr : getPurchaseRequests()) {
            if ("PENDING".equalsIgnoreCase(pr.getStatus())) pendingPurchases++;
        }

        stats.put("totalTitles", totalTitles);
        stats.put("totalCopies", totalCopies);
        stats.put("totalBranches", totalBranches);
        stats.put("totalUsers", totalUsers);
        stats.put("pendingTransfers", pendingTransfers);
        stats.put("pendingPurchases", pendingPurchases);
        stats.put("isInMemory", useFallbackInMemory);
        return stats;
    }

    public static void closeConnection() {
        // Cleanup resources
    }
}
