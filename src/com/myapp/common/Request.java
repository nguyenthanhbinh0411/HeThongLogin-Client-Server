package com.myapp.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private String action;
    private Map<String, String> data = new HashMap<>();

    public Request(String action) {
        this.action = action;
    }
    public String getAction() { return action; }
    public Map<String,String> getData() { return data; }
    public void put(String k, String v) { data.put(k, v); }
}
