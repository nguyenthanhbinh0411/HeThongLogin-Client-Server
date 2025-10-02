package com.myapp.client;

import javax.swing.*;

/**
 * Lớp chính để khởi chạy ứng dụng (phiên bản mới)
 * Đã được tái cấu trúc để sử dụng các giao diện riêng biệt
 */
public class ClientMain {
    
    /**
     * Phương thức main để khởi chạy ứng dụng
     * @param args Tham số dòng lệnh
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Thiết lập Look and Feel hiện đại
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            } catch (Exception e) {
                // Sử dụng look and feel mặc định nếu FlatLaf không có
                System.out.println("FlatLaf không khả dụng, sử dụng look and feel mặc định");
            }
            
            // Khởi tạo giao diện đăng nhập mới
            new LoginFrame().setVisible(true);
        });
    }
}