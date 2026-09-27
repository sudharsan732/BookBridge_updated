package com.bookbridge.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UserNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String message;
    private String type; // SUCCESS, ERROR, INFO
    private String createdAt;
    private boolean isRead;

    public UserNotification() {
        this.createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        this.type = "INFO";
        this.isRead = false;
    }

    public UserNotification(int id, String username, String message, String type) {
        this(id, username, message, type, new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), false);
    }

    public UserNotification(int id, String username, String message, String type, String createdAt, boolean isRead) {
        this.id = id;
        this.username = username;
        this.message = message;
        this.type = type != null ? type.toUpperCase() : "INFO";
        this.createdAt = createdAt != null ? createdAt : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        this.isRead = isRead;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type != null ? type.toUpperCase() : "INFO";
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    @Override
    public String toString() {
        return String.format("Notification #%d for @%s [%s]: %s", id, username, type, message);
    }
}
