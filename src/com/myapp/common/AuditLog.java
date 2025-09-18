package com.myapp.common;

import java.io.Serializable;

public class AuditLog implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private Integer userId;
    private String action;
    private String details;
    private String createdAt;

    // getters/setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
