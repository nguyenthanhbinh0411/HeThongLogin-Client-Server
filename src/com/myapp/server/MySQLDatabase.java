package com.myapp.server;

import com.myapp.common.User;
import com.myapp.common.LoginAttempt;
import com.myapp.common.AuditLog;
import com.myapp.common.Utils;

import java.sql.*;
import java.util.*;

/**
 * MySQL database implementation for user management system
 */
public class MySQLDatabase {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/user_management?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root"; // Change as needed
    private static final String DB_PASSWORD = "Thanhbinh12"; // Change as needed
    
    private Connection connection;
    
    public MySQLDatabase() throws SQLException {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Establish connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to MySQL database successfully!");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found", e);
        }
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // ---------- Users ----------
    public synchronized List<User> loadAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setFullName(rs.getString("full_name"));
                user.setEmail(rs.getString("email"));
                user.setAvatar(rs.getString("avatar"));
                user.setRole(rs.getString("role"));
                user.setStatus(rs.getString("status"));
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                user.setCreatedAt(createdAt != null ? createdAt.toString() : null);
                
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                user.setUpdatedAt(updatedAt != null ? updatedAt.toString() : null);
                
                Timestamp lastLogin = rs.getTimestamp("last_login");
                user.setLastLogin(lastLogin != null ? lastLogin.toString() : null);
                
                users.add(user);
            }
        }
        
        return users;
    }
    
    public synchronized Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setFullName(rs.getString("full_name"));
                    user.setEmail(rs.getString("email"));
                    user.setAvatar(rs.getString("avatar"));
                    user.setRole(rs.getString("role"));
                    user.setStatus(rs.getString("status"));
                    
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    user.setCreatedAt(createdAt != null ? createdAt.toString() : null);
                    
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    user.setUpdatedAt(updatedAt != null ? updatedAt.toString() : null);
                    
                    Timestamp lastLogin = rs.getTimestamp("last_login");
                    user.setLastLogin(lastLogin != null ? lastLogin.toString() : null);
                    
                    return Optional.of(user);
                }
            }
        }
        
        return Optional.empty();
    }
    
    public synchronized Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setFullName(rs.getString("full_name"));
                    user.setEmail(rs.getString("email"));
                    user.setAvatar(rs.getString("avatar"));
                    user.setRole(rs.getString("role"));
                    user.setStatus(rs.getString("status"));
                    
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    user.setCreatedAt(createdAt != null ? createdAt.toString() : null);
                    
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    user.setUpdatedAt(updatedAt != null ? updatedAt.toString() : null);
                    
                    Timestamp lastLogin = rs.getTimestamp("last_login");
                    user.setLastLogin(lastLogin != null ? lastLogin.toString() : null);
                    
                    return Optional.of(user);
                }
            }
        }
        
        return Optional.empty();
    }
    
    public synchronized User addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, avatar, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getAvatar());
            stmt.setString(6, user.getRole());
            stmt.setString(7, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        }
        
        return user;
    }
    
    public synchronized void updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET username=?, password_hash=?, full_name=?, email=?, avatar=?, role=?, status=?, last_login=? WHERE id=?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getAvatar());
            stmt.setString(6, user.getRole());
            stmt.setString(7, user.getStatus());
            
            if (user.getLastLogin() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(user.getLastLogin()));
            } else {
                stmt.setNull(8, Types.TIMESTAMP);
            }
            
            stmt.setInt(9, user.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating user failed, user not found with id: " + user.getId());
            }
        }
    }
    
    // ---------- Login attempts ----------
    public synchronized void addAttempt(LoginAttempt attempt) throws SQLException {
        String sql = "INSERT INTO login_attempts (user_id, username, success, ip) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (attempt.getUserId() != null) {
                stmt.setInt(1, attempt.getUserId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, attempt.getUsername());
            stmt.setBoolean(3, attempt.isSuccess());
            stmt.setString(4, attempt.getIp());
            
            stmt.executeUpdate();
        }
    }
    
    public synchronized int countRecentFailedAttempts(String username, int minutes) throws SQLException {
        String sql = "SELECT COUNT(*) FROM login_attempts WHERE username = ? AND success = false AND attempt_time > DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, minutes);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }
    
    // Method to count recent failed attempts for AuthService (simplified)
    public synchronized int countRecentFailed(String username) throws SQLException {
        // Count failed attempts in the last 60 minutes
        return countRecentFailedAttempts(username, 60);
    }
    
    // Method to reset failed login attempts count for a user (for successful login)
    public synchronized void resetFailedAttempts(String username) throws SQLException {
        String sql = "DELETE FROM login_attempts WHERE username = ? AND success = FALSE";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            int deletedRows = stmt.executeUpdate();
            System.out.println("Reset " + deletedRows + " failed login attempts for user: " + username);
        }
    }
    
    // ---------- Audit logs ----------
    public synchronized void addAudit(AuditLog audit) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, action, details) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (audit.getUserId() != null) {
                stmt.setInt(1, audit.getUserId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, audit.getAction());
            stmt.setString(3, audit.getDetails());
            
            stmt.executeUpdate();
        }
    }
    
    public synchronized List<AuditLog> loadAudits() throws SQLException {
        List<AuditLog> audits = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 1000";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                AuditLog audit = new AuditLog();
                audit.setId(rs.getInt("id"));
                audit.setUserId(rs.getObject("user_id", Integer.class));
                audit.setAction(rs.getString("action"));
                audit.setDetails(rs.getString("details"));
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                audit.setCreatedAt(createdAt != null ? createdAt.toString() : null);
                
                audits.add(audit);
            }
        }
        
        return audits;
    }
    
    // Get login attempts by username for user history
    public synchronized List<LoginAttempt> getLoginAttemptsByUsername(String username) throws SQLException {
        List<LoginAttempt> attempts = new ArrayList<>();
        String sql = "SELECT * FROM login_attempts WHERE username = ? ORDER BY attempt_time DESC LIMIT 50";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LoginAttempt attempt = new LoginAttempt();
                    attempt.setId(rs.getInt("id"));
                    attempt.setUserId(rs.getObject("user_id", Integer.class));
                    attempt.setUsername(rs.getString("username"));
                    attempt.setAttemptTime(rs.getString("attempt_time"));
                    attempt.setSuccess(rs.getBoolean("success"));
                    attempt.setIp(rs.getString("ip"));
                    attempts.add(attempt);
                }
            }
        }
        
        return attempts;
    }
    
    // Get audit logs by user ID for user history
    public synchronized List<AuditLog> getAuditLogsByUserId(int userId) throws SQLException {
        List<AuditLog> audits = new ArrayList<>();
    String sql = "SELECT * FROM audit_logs WHERE user_id = ? ORDER BY created_at DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AuditLog audit = new AuditLog();
                    audit.setId(rs.getInt("id"));
                    audit.setUserId(rs.getInt("user_id"));
                    audit.setAction(rs.getString("action"));
                    audit.setDetails(rs.getString("details"));
                    audit.setCreatedAt(rs.getString("created_at"));
                    audits.add(audit);
                }
            }
        }
        
        return audits;
    }
    
    // Get all login attempts for admin dashboard
    public synchronized List<LoginAttempt> getAllLoginAttempts() throws SQLException {
        List<LoginAttempt> attempts = new ArrayList<>();
        String sql = "SELECT * FROM login_attempts ORDER BY attempt_time DESC LIMIT 200";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                LoginAttempt attempt = new LoginAttempt();
                attempt.setId(rs.getInt("id"));
                attempt.setUserId(rs.getObject("user_id", Integer.class));
                attempt.setUsername(rs.getString("username"));
                attempt.setAttemptTime(rs.getString("attempt_time"));
                attempt.setSuccess(rs.getBoolean("success"));
                attempt.setIp(rs.getString("ip"));
                attempts.add(attempt);
            }
        }
        
        return attempts;
    }
}