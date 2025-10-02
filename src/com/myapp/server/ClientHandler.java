package com.myapp.server;

import com.myapp.common.*;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClientHandler implements Runnable {
    private Socket socket;
    private MySQLDatabase db;
    private AuthService auth;
    private Integer currentUserId; // Track current logged user

    public ClientHandler(Socket socket, MySQLDatabase db, AuthService auth) {
        this.socket = socket;
        this.db = db;
        this.auth = auth;
        this.currentUserId = null;
    }

    @Override
    public void run() {
        try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Object o;
            while ((o = ois.readObject()) != null) {
                if (!(o instanceof Request)) break;
                Request req = (Request)o;
                
                // Update activity if user is logged in
                if (currentUserId != null) {
                    ServerMain.updateUserActivity(currentUserId);
                }
                
                Response resp = handle(req, socket.getInetAddress().getHostAddress());
                oos.writeObject(resp);
                oos.flush();
            }
        } catch (EOFException eof) {
            // client closed connection - this is normal
            System.out.println("Client disconnected normally");
        } catch (java.net.SocketTimeoutException ste) {
            // socket timeout - client may be inactive
            System.out.println("Client connection timed out (inactive)");
        } catch (java.net.SocketException se) {
            // connection reset or broken - client crashed/killed
            System.out.println("Client connection lost: " + se.getMessage());
        } catch (Exception e) {
            System.out.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Remove user from online list when disconnecting
            if (currentUserId != null) {
                ServerMain.removeOnlineUser(currentUserId);
            }
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
    }

    private Response handle(Request req, String ip) {
        try {
            String a = req.getAction();
            Map<String,String> d = req.getData();
            switch (a) {
                case "LOGIN": {
                    String username = d.get("username");
                    String password = d.get("password");
                    AuthService.AuthResult ar = auth.login(username, password, ip);
                    if (!ar.ok) return new Response(false, ar.msg);
                    
                    // Track this user as online
                    currentUserId = ar.user.getId();
                    ServerMain.addOnlineUser(currentUserId, ar.user.getUsername(), this);
                    
                    Response r = new Response(true, "Đăng nhập thành công");
                    r.put("username", ar.user.getUsername());
                    r.put("role", ar.user.getRole());
                    r.put("fullName", ar.user.getFullName());
                    r.put("email", ar.user.getEmail());
                    r.put("avatar", ar.user.getAvatar() != null ? ar.user.getAvatar() : "");
                    r.put("id", String.valueOf(ar.user.getId()));
                    return r;
                }
                case "GET_PROFILE": {
                    String uid = d.get("id");
                    if (uid==null) return new Response(false,"Missing id");
                    try {
                        int id = Integer.parseInt(uid);
                        AuthService.ResponseWrapper rw = auth.getUserById(id);
                        if (!rw.ok) return new Response(false, rw.msg);
                        User user = (User) rw.payload;
                        Response r = new Response(true, "OK");
                        r.put("id", String.valueOf(user.getId()));
                        r.put("username", user.getUsername());
                        r.put("fullName", user.getFullName() != null ? user.getFullName() : "");
                        r.put("email", user.getEmail() != null ? user.getEmail() : "");
                        r.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
                        r.put("role", user.getRole());
                        r.put("status", user.getStatus());
                        return r;
                    } catch (NumberFormatException ex) { return new Response(false,"Invalid id"); }
                }
                case "ADMIN_LIST_USERS": {
                    AuthService.ResponseWrapper rw = auth.listUsers();
                    if (!rw.ok) return new Response(false, rw.msg);
                    Response r = new Response(true, "OK");
                    // put minimal user list in response data as CSV-ish with online status
                    @SuppressWarnings("unchecked")
                    List<User> users = (List<User>) rw.payload;
                    StringBuilder sb = new StringBuilder();
                    for (User u: users) {
                        String onlineStatus = ServerMain.isUserOnline(u.getId()) ? "ONLINE" : "OFFLINE";
                        String lastLogin = (u.getLastLogin() != null) ? u.getLastLogin() : "Chưa đăng nhập";
                        String fullName = (u.getFullName() != null) ? u.getFullName() : "";
                        String email = (u.getEmail() != null) ? u.getEmail() : "";
                        String avatar = (u.getAvatar() != null) ? u.getAvatar() : "";
                        String createdAt = (u.getCreatedAt() != null) ? u.getCreatedAt() : "";
                        
                        // Debug output
                        System.out.println("Sending user data: ID=" + u.getId() + 
                            ", Username=" + u.getUsername() + 
                            ", FullName=" + fullName + 
                            ", Email=" + email + 
                            ", Role=" + u.getRole() + 
                            ", Status=" + u.getStatus() + 
                            ", Online=" + onlineStatus);
                        
                        sb.append(u.getId()).append(",")
                          .append(u.getUsername()).append(",")
                          .append(fullName).append(",")
                          .append(email).append(",")
                          .append(avatar).append(",")
                          .append(u.getRole()).append(",")
                          .append(u.getStatus()).append(",")
                          .append(onlineStatus).append(",")
                          .append(lastLogin).append(",")
                          .append(createdAt).append(";");
                    }
                    r.put("users", sb.toString());
                    return r;
                }
                case "ADMIN_CREATE_USER": {
                    String username = d.get("username"), pwd = d.get("password"), full = d.get("fullName"), email = d.get("email"), avatar = d.get("avatar"), role = d.get("role");
                    AuthService.ResponseWrapper rw = auth.createUser(username, pwd, full, email, avatar, role);
                    return new Response(rw.ok, rw.msg);
                }
                case "ADMIN_RESET_PASSWORD": {
                    try {
                        String targetIdStr = d.get("targetUserId");
                        String providedNew = d.get("newPassword");
                        if (targetIdStr == null) return new Response(false, "Missing targetUserId");
                        if (providedNew == null || providedNew.trim().isEmpty()) return new Response(false, "Missing newPassword");
                        int targetId = Integer.parseInt(targetIdStr);

                        // Use auth.editUser to set the password (if supported by AuthService)
                        // pass null for avatar and role so only password is changed
                        AuthService.ResponseWrapper editRw = auth.editUser(targetId, null, null, null, null, providedNew);
                        if (!editRw.ok) return new Response(false, editRw.msg);

                        Response rr = new Response(true, "Password reset");
                        // Do not echo back the password for security reasons; client already has it
                        return rr;
                    } catch (NumberFormatException nfe) {
                        return new Response(false, "Invalid targetUserId");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return new Response(false, "Error resetting password: " + ex.getMessage());
                    }
                }
                case "REGISTER": {
                    String username = d.get("username"), pwd = d.get("password"), full = d.get("fullName"), email = d.get("email"), avatar = d.get("avatar"), role = d.get("role");
                    AuthService.ResponseWrapper rw = auth.createUser(username, pwd, full, email, avatar, role);
                    return new Response(rw.ok, rw.msg);
                }
                case "ADMIN_SET_STATUS": {
                    int id = Integer.parseInt(d.get("id"));
                    String status = d.get("status");
                    AuthService.ResponseWrapper rw = auth.setUserStatus(id, status);
                    return new Response(rw.ok, rw.msg);
                }
                case "ADMIN_EDIT_USER": {
                    int id = Integer.parseInt(d.get("id"));
                    String fullName = d.get("fullName");
                    String email = d.get("email");
                    String avatar = d.get("avatar");
                    String role = d.get("role");
                    String password = d.get("password");
                    AuthService.ResponseWrapper rw = auth.editUser(id, fullName, email, avatar, role, password);
                    return new Response(rw.ok, rw.msg);
                }
                case "ADMIN_GET_USER": {
                    int id = Integer.parseInt(d.get("id"));
                    AuthService.ResponseWrapper rw = auth.getUserById(id);
                    if (!rw.ok) return new Response(false, rw.msg);
                    
                    User user = (User) rw.payload;
                    Response r = new Response(true, "OK");
                    r.put("id", String.valueOf(user.getId()));
                    r.put("username", user.getUsername());
                    r.put("fullName", user.getFullName() != null ? user.getFullName() : "");
                    r.put("email", user.getEmail() != null ? user.getEmail() : "");
                    r.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
                    r.put("role", user.getRole());
                    r.put("status", user.getStatus());
                    return r;
                }
                case "GET_AUDITS": {
                    try {
                        List<AuditLog> la = db.loadAudits();
                        StringBuilder sb = new StringBuilder();
                        for (AuditLog aLog : la) {
                            String username = "System";
                            if (aLog.getUserId() != null) {
                                Optional<com.myapp.common.User> user = db.findById(aLog.getUserId());
                                if (user.isPresent()) {
                                    username = user.get().getUsername() + " (" + user.get().getRole() + ")";
                                } else {
                                    username = "User ID " + aLog.getUserId() + " (Deleted)";
                                }
                            }
                            
                            sb.append(aLog.getId()).append("|")
                              .append(username).append("|")
                              .append(aLog.getAction()).append("|")
                              .append(aLog.getDetails()).append("|")
                              .append(aLog.getCreatedAt()).append("\n");
                        }
                        Response rr = new Response(true, "OK");
                        rr.put("audits", sb.toString());
                        return rr;
                    } catch (java.sql.SQLException e) {
                        return new Response(false, "Database error loading audits: " + e.getMessage());
                    }
                }
                case "CHANGE_PASSWORD": {
                    if (currentUserId == null) return new Response(false, "Phải đăng nhập trước");
                    String oldPassword = d.get("oldPassword");
                    String newPassword = d.get("newPassword");
                    AuthService.ResponseWrapper rw = auth.changePassword(currentUserId, oldPassword, newPassword);
                    return new Response(rw.ok, rw.msg);
                }
                case "UPDATE_PROFILE": {    
                    if (currentUserId == null) return new Response(false, "Phải đăng nhập trước");
                    String fullName = d.get("fullName");
                    String email = d.get("email");
                    String avatar = d.get("avatar");
                    AuthService.ResponseWrapper rw = auth.updateProfile(currentUserId, fullName, email, avatar);
                    Response r = new Response(rw.ok, rw.msg);
                    if (rw.ok && rw.payload instanceof com.myapp.common.User) {
                        com.myapp.common.User updatedUser = (com.myapp.common.User) rw.payload;
                        r.put("id", String.valueOf(updatedUser.getId()));
                        r.put("username", updatedUser.getUsername());
                        r.put("fullName", updatedUser.getFullName());
                        r.put("email", updatedUser.getEmail());
                        r.put("avatar", updatedUser.getAvatar() != null ? updatedUser.getAvatar() : "");
                        r.put("role", updatedUser.getRole());
                        r.put("status", updatedUser.getStatus());
                    }
                    return r;
                }
                case "GET_USER_HISTORY": {
                    String username = d.get("username");
                    if (username == null || username.trim().isEmpty()) {
                        return new Response(false, "Username is required");
                    }
                    
                    try {
                        // Get user ID first
                        Optional<User> userOpt = db.findByUsername(username);
                        if (!userOpt.isPresent()) {
                            return new Response(false, "User not found");
                        }
                        
                        User user = userOpt.get();
                        StringBuilder sb = new StringBuilder();

                        // Get audit logs for this user only (no login attempt API usage)
                        List<AuditLog> audits = db.getAuditLogsByUserId(user.getId());
                        for (AuditLog audit : audits) {
                            String actionCode = audit.getAction();
                            String actionDesc = actionCode;
                            String resultDesc = "Thành công";

                            if ("LOGIN_SUCCESS".equalsIgnoreCase(actionCode)) {
                                actionDesc = "Đăng nhập thành công";
                                resultDesc = "Thành công";
                            } else if ("LOGIN_FAILED".equalsIgnoreCase(actionCode)) {
                                actionDesc = "Đăng nhập thất bại";
                                resultDesc = "Thất bại";
                            } else if ("LOGOUT".equalsIgnoreCase(actionCode)) {
                                actionDesc = "Đăng xuất";
                            } else if ("PROFILE_UPDATE".equalsIgnoreCase(actionCode)) {
                                actionDesc = "Cập nhật thông tin";
                            } else if ("PASSWORD_CHANGE".equalsIgnoreCase(actionCode)) {
                                actionDesc = "Đổi mật khẩu";
                            }

                            sb.append("AUDIT|")
                              .append(audit.getCreatedAt()).append("|")
                              .append(actionDesc).append("|")
                              .append(audit.getDetails() != null ? audit.getDetails() : "").append("|")
                              .append(resultDesc).append("\n");
                        }
                        
                        Response r = new Response(true, "User history retrieved");
                        r.put("history", sb.toString());
                        return r;
                    } catch (Exception e) {
                        return new Response(false, "Database error: " + e.getMessage());
                    }
                }
                case "GET_ONLINE_USERS": {
                    // Get list of online user IDs and return as comma-separated string
                    java.util.Set<Integer> onlineUserIds = ServerMain.getOnlineUserIds();
                    StringBuilder sb = new StringBuilder();
                    for (Integer userId : onlineUserIds) {
                        if (sb.length() > 0) sb.append(",");
                        sb.append(userId);
                    }
                    Response r = new Response(true, "OK");
                    r.put("onlineUserIds", sb.toString());
                    return r;
                }
                case "GET_ALL_LOGIN_LOGS": {
                    try {
                        // Get all login attempts from database
                        List<LoginAttempt> allAttempts = db.getAllLoginAttempts();
                        StringBuilder sb = new StringBuilder();
                        
                        for (LoginAttempt attempt : allAttempts) {
                            sb.append("LOGIN_ATTEMPT|")
                              .append(attempt.getAttemptTime()).append("|")
                              .append(attempt.getUsername()).append("|")
                              .append(attempt.isSuccess() ? "Đăng nhập thành công" : "Đăng nhập thất bại").append("|")
                              .append(attempt.getIp() != null ? attempt.getIp() : "N/A").append("|")
                              .append(attempt.isSuccess() ? "Thành công" : "Thất bại").append("\n");
                        }
                        
                        Response r = new Response(true, "Login logs retrieved");
                        r.put("loginLogs", sb.toString());
                        return r;
                    } catch (Exception e) {
                        return new Response(false, "Database error: " + e.getMessage());
                    }
                }
                case "PING": {
                    // Simple ping to keep connection alive and update activity
                    if (currentUserId != null) {
                        ServerMain.updateUserActivity(currentUserId);
                    }
                    return new Response(true, "PONG");
                }
                default:
                    return new Response(false, "Unknown action: " + a);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Server error: " + e.getMessage());
        }
    }
}
