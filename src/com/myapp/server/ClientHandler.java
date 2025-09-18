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
                    r.put("id", String.valueOf(ar.user.getId()));
                    return r;
                }
                case "GET_PROFILE": {
                    String uid = d.get("id");
                    if (uid==null) return new Response(false,"Missing id");
                    try {
                        int id = Integer.parseInt(uid);
                        return auth.listUsers().ok ? new Response(true,"OK") : new Response(false,"Error");
                    } catch (Exception ex) { return new Response(false,"Invalid id"); }
                }
                case "ADMIN_LIST_USERS": {
                    AuthService.ResponseWrapper rw = auth.listUsers();
                    if (!rw.ok) return new Response(false, rw.msg);
                    Response r = new Response(true, "OK");
                    // put user list in response data as CSV-ish with fullName, email and online status
                    @SuppressWarnings("unchecked")
                    List<User> users = (List<User>) rw.payload;
                    StringBuilder sb = new StringBuilder();
                    for (User u: users) {
                        String onlineStatus = ServerMain.isUserOnline(u.getId()) ? "ONLINE" : "OFFLINE";
                        // Format: id,username,fullName,email,role,status,onlineStatus
                        sb.append(u.getId()).append(",")
                          .append(u.getUsername()).append(",")
                          .append(u.getFullName() != null ? u.getFullName() : "").append(",")
                          .append(u.getEmail() != null ? u.getEmail() : "").append(",")
                          .append(u.getRole()).append(",")
                          .append(u.getStatus()).append(",")
                          .append(onlineStatus).append(";");
                    }
                    r.put("users", sb.toString());
                    return r;
                }
                case "ADMIN_CREATE_USER": {
                    String username = d.get("username"), pwd = d.get("password"), full = d.get("fullName"), email = d.get("email"), role = d.get("role");
                    AuthService.ResponseWrapper rw = auth.createUser(username, pwd, full, email, role);
                    return new Response(rw.ok, rw.msg);
                }
                case "REGISTER": {
                    String username = d.get("username"), pwd = d.get("password"), full = d.get("fullName"), email = d.get("email"), role = d.get("role");
                    AuthService.ResponseWrapper rw = auth.createUser(username, pwd, full, email, role);
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
                    String role = d.get("role");
                    String password = d.get("password");
                    AuthService.ResponseWrapper rw = auth.editUser(id, fullName, email, role, password);
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
                    AuthService.ResponseWrapper rw = auth.updateProfile(currentUserId, fullName, email);
                    Response r = new Response(rw.ok, rw.msg);
                    if (rw.ok && rw.payload instanceof com.myapp.common.User) {
                        com.myapp.common.User updatedUser = (com.myapp.common.User) rw.payload;
                        r.put("id", String.valueOf(updatedUser.getId()));
                        r.put("username", updatedUser.getUsername());
                        r.put("fullName", updatedUser.getFullName());
                        r.put("email", updatedUser.getEmail());
                        r.put("role", updatedUser.getRole());
                        r.put("status", updatedUser.getStatus());
                    }
                    return r;
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
