package com.myapp.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lớp chứa các phương thức tiện ích để tạo giao diện hiện đại
 * Bao gồm tạo nút, định dạng thời gian và các hiệu ứng
 */
public class UIUtils {
    
    /**
     * Tạo nút hiện đại với thiết kế bo tròn và hiệu ứng hover
     * @param text Văn bản hiển thị trên nút
     * @param bgColor Màu nền
     * @param textColor Màu văn bản
     * @param width Chiều rộng
     * @param height Chiều cao
     * @return JButton đã được tùy chỉnh
     */
    public static JButton createModernButton(String text, Color bgColor, Color textColor, int width, int height) {
        JButton button = new JButton(text) {
            private boolean isHovered = false;
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Vẽ nền với hiệu ứng hover
                Color currentBg = isHovered ? 
                    new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 200) : bgColor;
                g2.setColor(currentBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), UIConstants.BORDER_RADIUS, UIConstants.BORDER_RADIUS);
                
                // Vẽ văn bản
                g2.setColor(textColor);
                g2.setFont(UIConstants.FONT_BODY);
                FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(text)) / 2;
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, textX, textY);
                
                g2.dispose();
            }
        };
        
        button.setPreferredSize(new Dimension(width, height));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Thêm hiệu ứng hover
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                JButton btn = (JButton) e.getSource();
                btn.putClientProperty("isHovered", true);
                btn.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                JButton btn = (JButton) e.getSource();
                btn.putClientProperty("isHovered", false);
                btn.repaint();
            }
        });
        
        return button;
    }
    
    /**
     * Tạo nút hiện đại với kích thước mặc định
     * @param text Văn bản hiển thị
     * @param bgColor Màu nền
     * @param textColor Màu văn bản
     * @return JButton đã được tùy chỉnh
     */
    public static JButton createModernButton(String text, Color bgColor, Color textColor) {
        return createModernButton(text, bgColor, textColor, 120, UIConstants.BUTTON_HEIGHT);
    }
    
    /**
     * Định dạng thời gian lần đăng nhập cuối
     * @param lastLoginStr Chuỗi thời gian đăng nhập cuối
     * @return Chuỗi đã được định dạng
     */
    public static String formatLastLogin(String lastLoginStr) {
        if (lastLoginStr == null || lastLoginStr.isEmpty() || "Never".equals(lastLoginStr)) {
            return "Chưa đăng nhập";
        }
        
        try {
            LocalDateTime lastLogin = LocalDateTime.parse(lastLoginStr.replace(" ", "T"));
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(lastLogin, now);
            
            long minutes = duration.toMinutes();
            long hours = duration.toHours();
            long days = duration.toDays();
            
            if (minutes < 1) {
                return "Vừa mới";
            } else if (minutes < 60) {
                return minutes + " phút trước";
            } else if (hours < 24) {
                return hours + " giờ trước";
            } else if (days < 7) {
                return days + " ngày trước";
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return lastLogin.format(formatter);
            }
        } catch (Exception e) {
            return lastLoginStr; // Trả về chuỗi gốc nếu không parse được
        }
    }
    
    /**
     * Tạo JTextField với thiết kế hiện đại
     * @param placeholder Văn bản gợi ý
     * @return JTextField đã được tùy chỉnh
     */
    public static JTextField createModernTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(UIConstants.FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.DIVIDER),
            BorderFactory.createEmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_MD, 
                                          UIConstants.SPACING_SM, UIConstants.SPACING_MD)
        ));
        field.setPreferredSize(new Dimension(200, UIConstants.BUTTON_HEIGHT));
        
        // Thêm placeholder
        if (placeholder != null && !placeholder.isEmpty()) {
            field.setText(placeholder);
            field.setForeground(UIConstants.TEXT_SECONDARY);
            
            field.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    if (field.getText().equals(placeholder)) {
                        field.setText("");
                        field.setForeground(UIConstants.TEXT_PRIMARY);
                    }
                }
                
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    if (field.getText().isEmpty()) {
                        field.setText(placeholder);
                        field.setForeground(UIConstants.TEXT_SECONDARY);
                    }
                }
            });
        }
        
        return field;
    }
    
    /**
     * Tạo JPasswordField với thiết kế hiện đại
     * @param placeholder Văn bản gợi ý
     * @return JPasswordField đã được tùy chỉnh
     */
    public static JPasswordField createModernPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setFont(UIConstants.FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.DIVIDER),
            BorderFactory.createEmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_MD, 
                                          UIConstants.SPACING_SM, UIConstants.SPACING_MD)
        ));
        field.setPreferredSize(new Dimension(200, UIConstants.BUTTON_HEIGHT));
        
        return field;
    }
    
    /**
     * Tạo JLabel với thiết kế hiện đại
     * @param text Văn bản hiển thị
     * @param font Font chữ
     * @param color Màu văn bản
     * @return JLabel đã được tùy chỉnh
     */
    public static JLabel createModernLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }
    
    /**
     * Xác thực dữ liệu đầu vào người dùng
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @param fullName Họ tên
     * @param email Email
     * @return Thông báo lỗi hoặc null nếu hợp lệ
     */
    public static String validateUserInput(String username, String password, String fullName, String email) {
        // Check required fields
        if (username == null || username.trim().isEmpty()) {
            return "Username không được để trống!";
        }
        if (password == null || password.trim().isEmpty()) {
            return "Password không được để trống!";
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Full name không được để trống!";
        }
        if (email == null || email.trim().isEmpty()) {
            return "Email không được để trống!";
        }
        
        // Username validation
        if (username.length() < 3) {
            return "Username phải có ít nhất 3 ký tự!";
        }
        if (username.length() > 50) {
            return "Username không được quá 50 ký tự!";
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return "Username chỉ được chứa chữ cái, số và dấu gạch dưới!";
        }
        
        // Password validation
        if (password.length() < 6) {
            return "Password phải có ít nhất 6 ký tự!";
        }
        if (password.length() > 100) {
            return "Password không được quá 100 ký tự!";
        }
        
        // Email validation
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return "Email không đúng định dạng!";
        }
        if (email.length() > 100) {
            return "Email không được quá 100 ký tự!";
        }
        
        // Full name validation
        if (fullName.length() > 100) {
            return "Full name không được quá 100 ký tự!";
        }
        
        return null; // No validation errors
    }
    
    /**
     * Chuyển đổi vai trò từ tiếng Việt sang tiếng Anh
     * @param vietnameseRole Vai trò bằng tiếng Việt
     * @return Vai trò bằng tiếng Anh
     */
    public static String mapRoleToEnglish(String vietnameseRole) {
        switch (vietnameseRole) {
            case "NGƯỜI DÙNG": return "USER";
            case "QUẢN TRỊ VIÊN": return "ADMIN";
            default: return vietnameseRole;
        }
    }
    
    /**
     * Chuyển đổi vai trò từ tiếng Anh sang tiếng Việt
     * @param englishRole Vai trò bằng tiếng Anh
     * @return Vai trò bằng tiếng Việt
     */
    public static String mapRoleToVietnamese(String englishRole) {
        switch (englishRole) {
            case "USER": return "NGƯỜI DÙNG";
            case "ADMIN": return "QUẢN TRỊ VIÊN";
            default: return englishRole;
        }
    }
    
    /**
     * Chuyển đổi trạng thái từ tiếng Anh sang tiếng Việt
     * @param englishStatus Trạng thái bằng tiếng Anh
     * @return Trạng thái bằng tiếng Việt
     */
    public static String mapStatusToVietnamese(String englishStatus) {
        switch (englishStatus.toUpperCase()) {
            case "ACTIVE": return "HOẠT ĐỘNG";
            case "LOCKED": return "BỊ KHÓA";
            default: return englishStatus;
        }
    }
    
    /**
     * Chuyển đổi trạng thái online từ tiếng Anh sang tiếng Việt
     * @param englishOnline Trạng thái online bằng tiếng Anh
     * @return Trạng thái online bằng tiếng Việt
     */
    public static String mapOnlineToVietnamese(String englishOnline) {
        switch (englishOnline.toUpperCase()) {
            case "ONLINE": return "TRỰC TUYẾN";
            case "OFFLINE": return "NGOẠI TUYẾN";
            default: return englishOnline;
        }
    }
    
    /**
     * Dịch hành động audit log
     * @param action Hành động bằng tiếng Anh
     * @return Hành động bằng tiếng Việt
     */
    public static String translateAction(String action) {
        switch (action.toUpperCase()) {
            case "LOGIN_SUCCESS": return "Đăng nhập thành công";
            case "LOGIN_FAILED": return "Đăng nhập thất bại";
            case "LOGIN_BLOCKED": return "Đăng nhập bị chặn";
            case "CHANGE_PASSWORD_SUCCESS": return "Đổi mật khẩu thành công";
            case "CHANGE_PASSWORD_FAILED": return "Đổi mật khẩu thất bại";
            case "UPDATE_PROFILE": return "Cập nhật thông tin";
            case "USER_CREATED": return "Tạo người dùng";
            case "USER_UPDATED": return "Cập nhật người dùng";
            case "USER_STATUS_CHANGED": return "Thay đổi trạng thái";
            case "USER_LOCKED": return "Khóa tài khoản";
            case "USER_UNLOCKED": return "Mở khóa tài khoản";
            default: return action;
        }
    }
    
    // Ngăn không cho tạo instance
    private UIUtils() {}
}