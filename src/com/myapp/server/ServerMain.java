package com.myapp.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public class ServerMain {
    // Track online users with their connection info
    private static final ConcurrentHashMap<Integer, UserConnection> onlineUsers = new ConcurrentHashMap<>();
    
    // Inner class to track user connection details
    private static class UserConnection {
        String username;
        long lastActivity;
        ClientHandler handler;
        
        UserConnection(String username, ClientHandler handler) {
            this.username = username;
            this.handler = handler;
            this.lastActivity = System.currentTimeMillis();
        }
        
        void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
        
        boolean isExpired(long timeout) {
            return System.currentTimeMillis() - lastActivity > timeout;
        }
    }
    
    public static void addOnlineUser(int userId, String username, ClientHandler handler) {
        onlineUsers.put(userId, new UserConnection(username, handler));
        System.out.println("User " + username + " (ID: " + userId + ") is now ONLINE");
    }
    
    public static void removeOnlineUser(int userId) {
        UserConnection removed = onlineUsers.remove(userId);
        if (removed != null) {
            System.out.println("User " + removed.username + " (ID: " + userId + ") is now OFFLINE");
        }
    }
    
    public static void updateUserActivity(int userId) {
        UserConnection conn = onlineUsers.get(userId);
        if (conn != null) {
            conn.updateActivity();
        }
    }
    
    public static boolean isUserOnline(int userId) {
        return onlineUsers.containsKey(userId);
    }
    
    public static Set<Integer> getOnlineUserIds() {
        return onlineUsers.keySet();
    }
    
    // Cleanup expired connections (optional - can be called periodically)
    public static void cleanupExpiredConnections(long timeoutMs) {
        onlineUsers.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(timeoutMs)) {
                System.out.println("Removing expired connection for user ID: " + entry.getKey());
                return true;
            }
            return false;
        });
    }

    public static void main(String[] args) throws Exception {
        int port = 5555;
        MySQLDatabase db = null;
        
        try {
            db = new MySQLDatabase();
            
            // ensure admin exists
            if (!db.findByUsername("admin").isPresent()) {
                com.myapp.common.User admin = new com.myapp.common.User();
                admin.setUsername("admin");
                admin.setPasswordHash(com.myapp.common.Utils.sha256("admin123"));
                admin.setFullName("Administrator");
                admin.setEmail("admin@example.com");
                admin.setRole("ADMIN");
                db.addUser(admin);
                System.out.println("Created default admin / password admin123");
            }

            AuthService auth = new AuthService(db);

            // Start cleanup thread for expired connections
            Thread cleanupThread = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(30000); // Check every 30 seconds
                        cleanupExpiredConnections(120000); // Remove connections idle for 2 minutes
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            cleanupThread.setDaemon(true);
            cleanupThread.start();

            try (ServerSocket ss = new ServerSocket(port)) {
                System.out.println("Server listening on port " + port);
                while (true) {
                    Socket s = ss.accept();
                    System.out.println("Connected: " + s.getInetAddress());
                    
                    // Set socket timeout to detect dead connections
                    s.setSoTimeout(300000); // 5 minutes timeout
                    
                    ClientHandler h = new ClientHandler(s, db, auth);
                    new Thread(h).start();
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
}
