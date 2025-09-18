package com.myapp.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    // escape pipe char for file storage
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("|", "\\|");
    }
    public static String unesc(String s) {
        if (s == null) return "";
        return s.replace("\\|", "|").replace("\\\\", "\\");
    }
}
