package com.myapp.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;
    private Map<String,String> data = new HashMap<>();

    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Map<String,String> getData() { return data; }
    public void put(String k, String v) { data.put(k, v); }
}
