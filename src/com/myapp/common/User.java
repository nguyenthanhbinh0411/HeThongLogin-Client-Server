package com.myapp.common;

import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String role;    // USER / ADMIN
    private String status;  // ACTIVE / LOCKED
    private String createdAt;
    private String updatedAt;
    private String lastLogin;
    private boolean isOnline;  // Track online status

    public User() {}

    // Getter & Setter
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

    public String getPasswordHash() {
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastLogin() {
        return lastLogin;
    }
    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    public boolean isOnline() {
        return isOnline;
    }
    public void setOnline(boolean online) {
        isOnline = online;
    }

    @Override
    public String toString() {
        return id + "," + username + "," + fullName + "," + email + "," + role + "," + status
                + "," + createdAt + "," + updatedAt + "," + lastLogin;
    }
}
