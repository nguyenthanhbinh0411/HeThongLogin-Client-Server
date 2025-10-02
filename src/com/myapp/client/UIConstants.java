package com.myapp.client;

import java.awt.*;

/**
 * Lớp chứa các hằng số thiết kế chung cho toàn bộ ứng dụng
 * Bao gồm màu sắc, font chữ, khoảng cách và kích thước
 */
public class UIConstants {
    
    // ================ HỆ THỐNG THIẾT KẾ HIỆN ĐẠI ================
    
    // Bảng màu hiện đại
    public static final Color BG_PRIMARY = new Color(246, 248, 250);      // #F6F8FA - Màu nền chính
    public static final Color SURFACE = Color.WHITE;                       // #FFFFFF - Màu bề mặt
    public static final Color PRIMARY = new Color(25, 118, 210);           // #1976D2 - Màu chính
    public static final Color SUCCESS = new Color(46, 125, 50);            // #2E7D32 - Màu thành công
    public static final Color WARNING = new Color(255, 179, 0);            // #FFB300 - Màu cảnh báo
    public static final Color DANGER = new Color(211, 47, 47);             // #D32F2F - Màu nguy hiểm
    public static final Color TEXT_PRIMARY = new Color(17, 24, 39);        // #111827 - Màu văn bản chính
    public static final Color TEXT_SECONDARY = new Color(107, 114, 128);   // #6B7280 - Màu văn bản phụ
    public static final Color DIVIDER = new Color(230, 233, 238);          // #E6E9EE - Màu đường viền
    public static final Color HOVER_BG = new Color(237, 246, 255);         // #EDF6FF - Màu nền khi hover
    public static final Color SELECTED_BG = new Color(251, 252, 254);      // #FBFCFE - Màu nền khi chọn
    
    // Aliases for backward compatibility
    public static final Color PRIMARY_COLOR = PRIMARY;
    public static final Color SECONDARY_COLOR = new Color(156, 39, 176);
    public static final Color SUCCESS_COLOR = SUCCESS;
    public static final Color DANGER_COLOR = DANGER;
    public static final Color WARNING_COLOR = WARNING;
    public static final Color INFO_COLOR = new Color(2, 136, 209);
    public static final Color BACKGROUND_COLOR = BG_PRIMARY;
    
    // Typography hiện đại
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 28);      // Font tiêu đề
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 20);    // Font đầu đề
    public static final Font FONT_SUBHEADING = new Font("Segoe UI", Font.BOLD, 16); // Font đầu đề phụ
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);      // Font nội dung
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 13);     // Font nhỏ
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 13);     // Font nhãn
    
    // Font aliases for backward compatibility
    public static final Font TITLE_FONT = FONT_TITLE;
    public static final Font REGULAR_FONT = FONT_BODY;
    public static final Font BOLD_FONT = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font SMALL_FONT = FONT_SMALL;
    
    // Khoảng cách và kích thước hiện đại
    public static final int SPACING_XS = 4;        // Khoảng cách rất nhỏ
    public static final int SPACING_SM = 8;        // Khoảng cách nhỏ
    public static final int SPACING_MD = 16;       // Khoảng cách trung bình
    public static final int SPACING_LG = 24;       // Khoảng cách lớn
    public static final int SPACING_XL = 32;       // Khoảng cách rất lớn
    public static final int BORDER_RADIUS = 8;     // Bán kính bo góc
    public static final int BORDER_RADIUS_SM = 6;  // Bán kính bo góc nhỏ
    public static final int BUTTON_HEIGHT = 40;    // Chiều cao nút
    public static final int TABLE_ROW_HEIGHT = 48; // Chiều cao hàng bảng
    
    // Spacing aliases for backward compatibility
    public static final int SPACING_SMALL = SPACING_SM;
    public static final int SPACING_MEDIUM = SPACING_MD;
    public static final int SPACING_LARGE = SPACING_LG;
    
    // Phương thức tiện ích
    private UIConstants() {
        // Ngăn không cho tạo instance
    }
}
