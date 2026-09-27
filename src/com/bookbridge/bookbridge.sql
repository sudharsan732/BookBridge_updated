CREATE DATABASE IF NOT EXISTS bookbridge;
USE bookbridge;

CREATE TABLE IF NOT EXISTS branches (
    branch_id INT PRIMARY KEY,
    branch_name VARCHAR(100),
    location VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    branch_id INT,
    created_at VARCHAR(50),
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
);

CREATE TABLE IF NOT EXISTS books (
    book_id INT PRIMARY KEY,
    title VARCHAR(100),
    author VARCHAR(100),
    available_copies INT,
    branch_id INT,
    category VARCHAR(100) DEFAULT 'General',
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
);

CREATE TABLE IF NOT EXISTS transfer_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    book_name VARCHAR(100),
    from_branch VARCHAR(100),
    to_branch VARCHAR(100),
    requester_name VARCHAR(100) DEFAULT 'Member',
    quantity INT DEFAULT 1,
    status VARCHAR(50) DEFAULT 'PENDING',
    request_date VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS purchase_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    book_name VARCHAR(100),
    author VARCHAR(100) DEFAULT 'Unknown',
    category VARCHAR(100) DEFAULT 'General',
    requested_branch VARCHAR(100) DEFAULT 'Guindy Library',
    requester_name VARCHAR(100) DEFAULT 'Member',
    status VARCHAR(50) DEFAULT 'PENDING',
    request_date VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS borrow_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    username VARCHAR(50) NOT NULL,
    book_id INT NOT NULL,
    book_title VARCHAR(100) NOT NULL,
    branch_id INT NOT NULL,
    borrow_date VARCHAR(50),
    return_date VARCHAR(50),
    status VARCHAR(20) DEFAULT 'BORROWED'
);

CREATE TABLE IF NOT EXISTS user_notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    message VARCHAR(255) NOT NULL,
    type VARCHAR(20) DEFAULT 'INFO',
    created_at VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE
);

-- Seed Branches
INSERT IGNORE INTO branches VALUES
(1, 'Guindy Library', 'Guindy'),
(2, 'Adyar Library', 'Adyar'),
(3, 'Velachery Library', 'Velachery');

-- Seed Default Accounts
INSERT IGNORE INTO users (user_id, username, password, full_name, role, branch_id, created_at) VALUES
(1, 'admin', 'admin123', 'System Administrator', 'ADMIN', 1, '2026-09-09 10:00'),
(2, 'purushothaman', 'user123', 'Purushothaman', 'MEMBER', 1, '2026-09-09 10:00'),
(3, 'alice', 'user123', 'Alice Johnson', 'MEMBER', 2, '2026-09-09 10:00');

-- Seed Sample Books
INSERT IGNORE INTO books (book_id, title, author, available_copies, branch_id, category) VALUES
(101, 'Clean Code', 'Robert C. Martin', 5, 1, 'Software Engineering'),
(102, 'Java: The Complete Reference', 'Herbert Schildt', 3, 1, 'Programming'),
(103, 'Data Structures & Algorithms', 'Mark Allen Weiss', 4, 2, 'Computer Science'),
(104, 'Operating System Concepts', 'Silberschatz & Galvin', 2, 2, 'Systems'),
(105, 'Computer Networks', 'Andrew S. Tanenbaum', 6, 3, 'Networking'),
(106, 'Database System Concepts', 'Korth & Sudarshan', 1, 3, 'Databases'),
(107, 'Design Patterns (GoF)', 'Erich Gamma et al.', 4, 1, 'Architecture'),
(108, 'Designing Data-Intensive Applications', 'Martin Kleppmann', 3, 2, 'Distributed Systems');
