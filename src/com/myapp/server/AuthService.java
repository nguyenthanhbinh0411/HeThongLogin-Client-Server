package com.myapp.server;

import com.myapp.common.User;
import com.myapp.common.LoginAttempt;
import com.myapp.common.AuditLog;
import com.myapp.common.Utils;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Business logic: login, lockout policy, admin operations
 */
public class AuthService {
    private final MySQLDatabase db;
    private final int MAX_FAILED = 5;

    public AuthService(MySQLDatabase db) {
        this.db = db;
    }

    public synchronized AuthResult login(String username, String password, String ip) throws SQLException {
        Optional<User> ou = db.findByUsername(username);
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setIp(ip);

        if (!ou.isPresent()) {
            attempt.setSuccess(false);
            db.addAttempt(attempt);
            db.addAudit(createAudit(null, "LOGIN_FAILED", "Unknown user " + username));
            return AuthResult.fail("Người dùng không tồn tại");
        }
        User u = ou.get();
        if (!"ACTIVE".equalsIgnoreCase(u.getStatus())) {
            attempt.setSuccess(false);
            attempt.setUserId(u.getId());
            db.addAttempt(attempt);
            db.addAudit(createAudit(u.getId(), "LOGIN_BLOCKED", "Attempt when status=" + u.getStatus()));
            return AuthResult.fail("Tài khoản không được phép đăng nhập: " + u.getStatus());
        }
        String hashed = Utils.sha256(password);
        if (!hashed.equals(u.getPasswordHash())) {
            attempt.setSuccess(false);
            attempt.setUserId(u.getId());
            db.addAttempt(attempt);
            db.addAudit(createAudit(u.getId(), "LOGIN_FAILED", "Wrong password"));
            
            // check failed count AFTER adding this failed attempt
            int failed = countRecentFailed(username);
            if (failed >= MAX_FAILED) {
                u.setStatus("LOCKED");
                db.updateUser(u);
                db.addAudit(createAudit(u.getId(), "LOCKED", "Too many failed attempts (5 times)"));
                return AuthResult.fail("Tài khoản bị khóa do 5 lần đăng nhập sai liên tiếp");
            }
            return AuthResult.fail("Sai mật khẩu. Lần đăng nhập sai liên tiếp: " + failed + "/5");
        }
        // success
        attempt.setSuccess(true);
        attempt.setUserId(u.getId());
        db.addAttempt(attempt);
        
        // Reset failed login attempts count on successful login
        db.resetFailedAttempts(username);
        
        // update last login
        u.setLastLogin(Utils.now());
        db.updateUser(u);
        db.addAudit(createAudit(u.getId(), "LOGIN_SUCCESS", "Login from " + ip));
        return AuthResult.success(u);
    }

    private int countRecentFailed(String username) {
        // Use MySQLDatabase to count recent failed attempts
        try {
            return db.countRecentFailed(username);
        } catch (SQLException e) {
            System.err.println("Error counting recent failed attempts: " + e.getMessage());
            return 0; // Return 0 on error to avoid blocking users due to database issues
        }
    }

    private AuditLog createAudit(Integer userId, String action, String details) {
        AuditLog a = new AuditLog();
        a.setUserId(userId);
        a.setAction(action);
        a.setDetails(details);
        a.setCreatedAt(Utils.now());
        return a;
    }

    // Admin operations
    public synchronized ResponseWrapper listUsers() {
        try {
            return ResponseWrapper.okWith("users", db.loadAllUsers());
        } catch (Exception e) { return ResponseWrapper.error(e.getMessage()); }
    }

    public synchronized ResponseWrapper createUser(String username, String password, String fullName, String email, String role) {
        try {
            if (db.findByUsername(username).isPresent()) return ResponseWrapper.error("Username existed");
            User u = new User();
            u.setUsername(username);
            u.setPasswordHash(Utils.sha256(password));
            u.setFullName(fullName);
            u.setEmail(email);
            u.setRole(role==null?"USER":role);
            u.setStatus("ACTIVE");
            db.addUser(u);
            db.addAudit(createAudit(null, "ADMIN_CREATE_USER", "Created " + username));
            return ResponseWrapper.ok("Tạo user thành công");
        } catch (Exception e) { return ResponseWrapper.error(e.getMessage()); }
    }

    public synchronized ResponseWrapper setUserStatus(int id, String status) {
        try {
            Optional<User> ou = db.findById(id);
            if (!ou.isPresent()) return ResponseWrapper.error("User not found");
            User u = ou.get();
            u.setStatus(status);
            db.updateUser(u);
            db.addAudit(createAudit(u.getId(), "ADMIN_SET_STATUS", "Status -> " + status));
            return ResponseWrapper.ok("Đã đổi trạng thái");
        } catch (Exception e) { return ResponseWrapper.error(e.getMessage()); }
    }

    public synchronized ResponseWrapper editUser(int id, String fullName, String email, String role, String password) {
        try {
            Optional<User> ou = db.findById(id);
            if (!ou.isPresent()) return ResponseWrapper.error("User not found");
            User u = ou.get();
            
            // Update fields if provided
            if (fullName != null && !fullName.trim().isEmpty()) {
                u.setFullName(fullName);
            }
            if (email != null && !email.trim().isEmpty()) {
                u.setEmail(email);
            }
            if (role != null && !role.trim().isEmpty()) {
                u.setRole(role);
            }
            if (password != null && !password.trim().isEmpty()) {
                u.setPasswordHash(Utils.sha256(password));
            }
            
            u.setUpdatedAt(Utils.now());
            db.updateUser(u);
            db.addAudit(createAudit(u.getId(), "ADMIN_EDIT_USER", "User information updated"));
            return ResponseWrapper.ok("Cập nhật user thành công");
        } catch (Exception e) { return ResponseWrapper.error(e.getMessage()); }
    }

    public synchronized ResponseWrapper getUserById(int id) {
        try {
            Optional<User> ou = db.findById(id);
            if (!ou.isPresent()) {
                return ResponseWrapper.error("User không tồn tại");
            }
            return ResponseWrapper.okWith("user", ou.get());
        } catch (Exception e) { return ResponseWrapper.error(e.getMessage()); }
    }

    public synchronized ResponseWrapper changePassword(int userId, String oldPassword, String newPassword) {
        try {
            Optional<User> ou = db.findById(userId);
            if (!ou.isPresent()) {
                return ResponseWrapper.error("User không tồn tại");
            }
            
            User user = ou.get();
            String oldHashed = Utils.sha256(oldPassword);
            
            // Verify old password
            if (!oldHashed.equals(user.getPasswordHash())) {
                db.addAudit(createAudit(userId, "CHANGE_PASSWORD_FAILED", "Wrong old password"));
                return ResponseWrapper.error("Mật khẩu cũ không đúng");
            }
            
            // Update to new password
            String newHashed = Utils.sha256(newPassword);
            user.setPasswordHash(newHashed);
            user.setUpdatedAt(Utils.now());
            db.updateUser(user);
            
            db.addAudit(createAudit(userId, "CHANGE_PASSWORD_SUCCESS", "Password changed successfully"));
            return ResponseWrapper.ok("Đổi mật khẩu thành công");
        } catch (Exception e) { 
            return ResponseWrapper.error("Lỗi server: " + e.getMessage()); 
        }
    }

    public synchronized ResponseWrapper updateProfile(int userId, String fullName, String email) {
        try {
            Optional<User> ou = db.findById(userId);
            if (!ou.isPresent()) {
                return ResponseWrapper.error("User không tồn tại");
            }
            
            User user = ou.get();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setUpdatedAt(Utils.now());
            db.updateUser(user);
            
            db.addAudit(createAudit(userId, "UPDATE_PROFILE", "Profile updated: " + fullName + ", " + email));
            return ResponseWrapper.okWith("user", user);
        } catch (Exception e) { 
            return ResponseWrapper.error("Lỗi server: " + e.getMessage()); 
        }
    }

    public static class AuthResult {
        public boolean ok;
        public String msg;
        public User user;
        public static AuthResult success(User u) { AuthResult r = new AuthResult(); r.ok=true; r.user=u; return r;}
        public static AuthResult fail(String m) { AuthResult r = new AuthResult(); r.ok=false; r.msg=m; return r; }
    }

    public static class ResponseWrapper {
        public boolean ok;
        public String msg;
        public Object payload;
        public static ResponseWrapper ok(String m){ ResponseWrapper r=new ResponseWrapper(); r.ok=true; r.msg=m; return r; }
        public static ResponseWrapper okWith(String key, Object o){ ResponseWrapper r=new ResponseWrapper(); r.ok=true; r.msg="OK"; r.payload=o; return r; }
        public static ResponseWrapper error(String m){ ResponseWrapper r=new ResponseWrapper(); r.ok=false; r.msg=m; return r; }
    }
}
