package com.myapp.common;

import java.io.Serializable;

public class LoginAttempt implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private Integer userId; // nullable
    private String username;
    private String attemptTime;
    private boolean success;
    private String ip;

    // getters/setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAttemptTime() { return attemptTime; }
    public void setAttemptTime(String attemptTime) { this.attemptTime = attemptTime; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}
