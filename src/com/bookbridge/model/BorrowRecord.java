package com.bookbridge.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BorrowRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String username;
    private int bookId;
    private String bookTitle;
    private int branchId;
    private String branchName;
    private String borrowDate;
    private String returnDate;
    private String status; // BORROWED, RETURNED

    public BorrowRecord() {
        this.status = "BORROWED";
        this.borrowDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
    }

    public BorrowRecord(int id, int userId, String username, int bookId, String bookTitle, int branchId, String branchName, String borrowDate, String returnDate, String status) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.branchId = branchId;
        this.branchName = branchName;
        this.borrowDate = borrowDate != null ? borrowDate : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        this.returnDate = returnDate;
        this.status = status != null ? status : "BORROWED";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(String borrowDate) {
        this.borrowDate = borrowDate;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format(
            "BorrowRecord #%d: User @%s borrowed '%s' [Book #%d] at %s on %s | Status: %s",
            id, username, bookTitle, bookId, (branchName != null ? branchName : "Branch " + branchId), borrowDate, status
        );
    }
}
