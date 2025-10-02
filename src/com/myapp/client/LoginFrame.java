package com.myapp.client;

import com.myapp.common.Request;
import com.myapp.common.Response;
import com.myapp.common.User;

import javax.swing.*;
import java.awt.*;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Giao diện đăng nhập với thiết kế hiện đại sử dụng FlatLaf
 * Bao gồm form đăng nhập và xử lý xác thực người dùng
 */
public class LoginFrame extends JFrame {
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private NetworkClient networkClient;
    
    public LoginFrame() {
        initializeComponents();
        setupLayout();
        setupEventListeners();
        
        setTitle("Đăng nhập hệ thống");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(400, 300));
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Kết nối tới server
        connectToServer();
    }
    
    /**
     * Khởi tạo các thành phần giao diện
     */
    private void initializeComponents() {
        // Accent color
        Color accentColor = new Color(30, 136, 229); // #1E88E5
        
        usernameField = createStyledTextField("Tên đăng nhập");
        passwordField = createStyledPasswordField("Mật khẩu");
        
        loginButton = createStyledPrimaryButton("Đăng nhập", accentColor);
        registerButton = createStyledSecondaryButton("Đăng ký", accentColor);
    }
    
    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField(20);
        field.setPreferredSize(new Dimension(250, 40));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setBackground(Color.WHITE);
        
        // Placeholder effect
        field.setText(placeholder);
        field.setForeground(new Color(156, 163, 175));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(new Color(31, 41, 55));
                }
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(30, 136, 229), 2, true),
                    BorderFactory.createEmptyBorder(7, 11, 7, 11)
                ));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(156, 163, 175));
                }
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        
        return field;
    }
    
    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField(20);
        field.setPreferredSize(new Dimension(250, 40));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setBackground(Color.WHITE);
        field.setEchoChar('\u2022');
        
        // Placeholder effect
        field.setForeground(new Color(156, 163, 175));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setForeground(new Color(31, 41, 55));
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(30, 136, 229), 2, true),
                    BorderFactory.createEmptyBorder(7, 11, 7, 11)
                ));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        
        return field;
    }
    
    private JButton createStyledPrimaryButton(String text, Color accentColor) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(180, 40));
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(accentColor);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(accentColor.darker());
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(accentColor);
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setBackground(accentColor.darker().darker());
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                button.setBackground(accentColor.darker());
            }
        });
        
        return button;
    }
    
    private JButton createStyledSecondaryButton(String text, Color accentColor) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(180, 40));
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(Color.WHITE);
        button.setForeground(accentColor);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 1, true),
            BorderFactory.createEmptyBorder(7, 15, 7, 15)
        ));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(248, 250, 252));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(Color.WHITE);
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(241, 245, 249));
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(248, 250, 252));
            }
        });
        
        return button;
    }
    
    /**
     * Thiết lập bố cục giao diện
     */
    private void setupLayout() {
        // Accent color
        Color accentColor = new Color(30, 136, 229); // #1E88E5
        
        // Panel chính với màu nền xám nhạt
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(248, 250, 252)); // Very light gray
        
        // Card panel với shadow và bo góc
        JPanel cardPanel = new JPanel(new GridBagLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
            BorderFactory.createEmptyBorder(32, 32, 32, 32)
        ));
        cardPanel.setPreferredSize(new Dimension(520, 400)); // Card width ~520px
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 20, 0); // Spacing 20px between rows
        gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        
        // Tiêu đề lớn, semibold, căn giữa
        JLabel titleLabel = new JLabel("Đăng nhập");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28)); // 28px semibold
        titleLabel.setForeground(new Color(31, 41, 55)); // #1f2937
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        cardPanel.add(titleLabel, gbc);
        
        // Label tên đăng nhập (13-14px, #4b5563)
        gbc.gridwidth = 1; gbc.insets = new Insets(0, 0, 8, 10); // 8px spacing to input
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 1;
        JLabel usernameLabel = new JLabel("Tên đăng nhập:");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameLabel.setForeground(new Color(75, 85, 99)); // #4b5563
        cardPanel.add(usernameLabel, gbc);
        
        // Trường tên đăng nhập
        gbc.gridx = 1; gbc.insets = new Insets(0, 0, 20, 0); // 20px to next row
        cardPanel.add(usernameField, gbc);
        
        // Label mật khẩu
        gbc.gridx = 0; gbc.gridy = 2; gbc.insets = new Insets(0, 0, 8, 10);
        JLabel passwordLabel = new JLabel("Mật khẩu:");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordLabel.setForeground(new Color(75, 85, 99));
        cardPanel.add(passwordLabel, gbc);
        
        // Trường mật khẩu
        gbc.gridx = 1; gbc.insets = new Insets(0, 0, 20, 0);
        cardPanel.add(passwordField, gbc);
        
        // Checkbox hiện mật khẩu (inline)
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.insets = new Insets(0, 0, 24, 0); // 24px to buttons
        gbc.anchor = GridBagConstraints.WEST;
        JCheckBox showPasswordCheck = new JCheckBox("Hiển thị mật khẩu");
        showPasswordCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        showPasswordCheck.setBackground(Color.WHITE);
        showPasswordCheck.setForeground(new Color(107, 114, 128)); // #6b7280 muted
        showPasswordCheck.addActionListener(e -> {
            passwordField.setEchoChar(showPasswordCheck.isSelected() ? (char)0 : '\u2022');
        });
        cardPanel.add(showPasswordCheck, gbc);
        
        // Panel nút ngang, căn giữa
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0)); // 16px spacing
        buttonPanel.setOpaque(false);
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        cardPanel.add(buttonPanel, gbc);
        
        // Căn giữa cardPanel trong mainPanel
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.gridx = 0; mainGbc.gridy = 0; mainGbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(cardPanel, mainGbc);
        
        add(mainPanel);
    }
    
    /**
     * Thiết lập các sự kiện cho giao diện
     */
    private void setupEventListeners() {
        // Sự kiện đăng nhập
        ActionListener loginAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        };
        
        loginButton.addActionListener(loginAction);
        
        // Sự kiện đăng ký
        registerButton.addActionListener(e -> openRegisterDialog());
        
        // Enter key để đăng nhập
        KeyAdapter enterKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        };
        
        usernameField.addKeyListener(enterKeyListener);
        passwordField.addKeyListener(enterKeyListener);
    }
    
    /**
     * Kết nối tới server
     */
    private void connectToServer() {
        try {
            networkClient = new NetworkClient("localhost", 5555);
            networkClient.connect();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Không thể kết nối tới server: " + e.getMessage(), 
                "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Thực hiện đăng nhập
     */
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        // Kiểm tra dữ liệu đầu vào
        if (username.isEmpty() || username.equals("Tên đăng nhập")) {
            JOptionPane.showMessageDialog(this, 
                "Vui lòng nhập tên đăng nhập!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            usernameField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Vui lòng nhập mật khẩu!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            passwordField.requestFocus();
            return;
        }
        
        // Vô hiệu hóa nút đăng nhập trong khi xử lý
        loginButton.setEnabled(false);
        loginButton.setText("Đang đăng nhập...");
        
        // Thực hiện đăng nhập trong thread riêng
        new Thread(() -> {
            try {
                if (networkClient == null) {
                    connectToServer();
                }
                
                Request request = new Request("LOGIN");
                request.put("username", username);
                request.put("password", password);
                Response response = networkClient.send(request);
                
                SwingUtilities.invokeLater(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Đăng nhập");
                    
                    if (response.isSuccess()) {
                        // Tạo User từ response data
                        User user = new User();
                        user.setUsername(response.getData().get("username"));
                        user.setFullName(response.getData().get("fullName"));
                        user.setEmail(response.getData().get("email"));
                        user.setAvatar(response.getData().get("avatar"));
                        user.setRole(response.getData().get("role"));
                        
                        // Set ID if available
                        String idStr = response.getData().get("id");
                        if (idStr != null && !idStr.isEmpty()) {
                            try {
                                user.setId(Integer.parseInt(idStr));
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid user ID: " + idStr);
                            }
                        }
                        
                        JOptionPane.showMessageDialog(this, 
                            "Đăng nhập thành công! Chào mừng " + user.getFullName(), 
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        
                        // Chuyển đến giao diện tương ứng
                        if ("ADMIN".equals(user.getRole())) {
                            openAdminInterface(user, networkClient);
                        } else {
                            openUserInterface(user, networkClient);
                        }
                        
                        dispose(); // Đóng cửa sổ đăng nhập
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            response.getMessage(), 
                            "Lỗi đăng nhập", JOptionPane.ERROR_MESSAGE);
                        passwordField.setText("");
                        passwordField.requestFocus();
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Đăng nhập");
                    
                    JOptionPane.showMessageDialog(this, 
                        "Lỗi kết nối: " + e.getMessage(), 
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    /**
     * Mở giao diện admin
     * @param user Thông tin người dùng
     * @param client Kết nối mạng
     */
    private void openAdminInterface(User user, NetworkClient client) {
        SwingUtilities.invokeLater(() -> {
            AdminFrame adminFrame = new AdminFrame(user, client);
            adminFrame.setVisible(true);
        });
    }
    
    /**
     * Mở giao diện user
     * @param user Thông tin người dùng
     * @param client Kết nối mạng
     */
    private void openUserInterface(User user, NetworkClient client) {
        SwingUtilities.invokeLater(() -> {
            UserFrame userFrame = new UserFrame(user, client);
            userFrame.setVisible(true);
        });
    }
    
    /**
     * Mở dialog đăng ký tài khoản mới
     */
    private void openRegisterDialog() {
        RegisterDialog registerDialog = new RegisterDialog(this, networkClient);
        registerDialog.setVisible(true);
    }
    
    /**
     * Phương thức main để chạy ứng dụng
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
            
            new LoginFrame().setVisible(true);
        });
    }
}

/**
 * Dialog đăng ký tài khoản mới
 */
class RegisterDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField fullNameField;
    private JTextField emailField;
    private JTextField avatarField;
    private JButton previewButton;
    private JLabel avatarPreview;
    private JComboBox<String> roleComboBox;
    private JButton registerButton;
    private JButton cancelButton;
    private NetworkClient networkClient;
    
    public RegisterDialog(JFrame parent, NetworkClient client) {
        super(parent, "Đăng ký tài khoản mới", true);
        this.networkClient = client;
        
        initializeComponents();
        setupLayout();
        setupEventListeners();
        
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }
    
    private void initializeComponents() {
        usernameField = createStyledTextField("Tên đăng nhập");
        passwordField = createStyledPasswordField("Mật khẩu");
        confirmPasswordField = createStyledPasswordField("Xác nhận mật khẩu");
        fullNameField = createStyledTextField("Họ và tên");
        emailField = createStyledTextField("Email");
        avatarField = createStyledTextField("Đường dẫn ảnh (tùy chọn)");
        
        // Avatar preview (no outer border, larger for higher quality)
        avatarPreview = new JLabel("Không có ảnh");
        avatarPreview.setPreferredSize(new Dimension(200, 200));
        avatarPreview.setHorizontalAlignment(SwingConstants.CENTER);
        avatarPreview.setFont(UIConstants.FONT_SMALL);
        avatarPreview.setBackground(UIConstants.SURFACE);
        avatarPreview.setOpaque(false); // let the dialog background show through
        
        previewButton = new JButton("Xem trước");
        previewButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        previewButton.setForeground(Color.WHITE);
        previewButton.setBackground(UIConstants.PRIMARY);
        previewButton.setFocusPainted(false);
        previewButton.setBorderPainted(false);
        previewButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        roleComboBox = new JComboBox<>(new String[]{"NGƯỜI DÙNG"});
        styleComboBox(roleComboBox);
        roleComboBox.setEnabled(false); // Không cho phép chọn vai trò khác
        
        registerButton = createStyledPrimaryButton("Đăng ký", new Color(45, 156, 219));
        cancelButton = createStyledSecondaryButton("Hủy", new Color(45, 156, 219));
    }
    
    private void setupLayout() {
        // Panel chính với màu nền xám nhạt
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(247, 250, 252)); // #f7fafc
        
        // Card panel với shadow và bo góc
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
            BorderFactory.createEmptyBorder(28, 28, 28, 28)
        ));
        cardPanel.setPreferredSize(new Dimension(720, 600)); // Card width ~720px
        
        // Tiêu đề lớn, semibold, căn giữa
        JLabel titleLabel = new JLabel("Đăng ký tài khoản mới");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28)); // 28px semibold
        titleLabel.setForeground(new Color(31, 41, 55)); // #1f2937
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cardPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Content panel với 2 cột: avatar trái, form phải
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(32, 0, 0, 0)); // Margin top
        
        // Left panel - Avatar section
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);
        
        JLabel avatarTitleLabel = new JLabel("Avatar");
        avatarTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        avatarTitleLabel.setForeground(new Color(100, 100, 100));
        avatarTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(avatarTitleLabel);
        leftPanel.add(Box.createVerticalStrut(15));
        
        // Avatar preview with circular border
        JPanel avatarContainer = new JPanel(new GridBagLayout());
        avatarContainer.setBackground(Color.WHITE);
        
        avatarPreview.setPreferredSize(new Dimension(200, 200));
        avatarPreview.setHorizontalAlignment(JLabel.CENTER);
        avatarPreview.setText("👤");
        avatarPreview.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
        avatarPreview.setOpaque(false);
        
        avatarContainer.add(avatarPreview);
        avatarContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(avatarContainer);

        // Add preview button centered below avatar
        leftPanel.add(Box.createVerticalStrut(12));
        JPanel previewBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        previewBtnPanel.setBackground(Color.WHITE);
        previewButton.setPreferredSize(new Dimension(120, 36));
        previewBtnPanel.add(previewButton);
        previewBtnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(previewBtnPanel);
        
        contentPanel.add(leftPanel, BorderLayout.WEST);
        
        // Right panel - Form fields
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Tên đăng nhập
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Tên đăng nhập:");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        usernameLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(usernameLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(usernameField, gbc);
        
        // Mật khẩu
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel passwordLabel = new JLabel("Mật khẩu:");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passwordLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(passwordLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(passwordField, gbc);
        
        // Xác nhận mật khẩu
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel confirmPasswordLabel = new JLabel("Xác nhận mật khẩu:");
        confirmPasswordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        confirmPasswordLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(confirmPasswordLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(confirmPasswordField, gbc);
        
        // Họ và tên
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel fullNameLabel = new JLabel("Họ tên:");
        fullNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        fullNameLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(fullNameLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(fullNameField, gbc);
        
        // Email
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        emailLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(emailLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(emailField, gbc);
        
        // Avatar URL
        gbc.gridx = 0; gbc.gridy = 5;
        JLabel avatarLabel = new JLabel("Avatar URL:");
        avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        avatarLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(avatarLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(avatarField, gbc);
        
        // Vai trò
        gbc.gridx = 0; gbc.gridy = 6; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel roleLabel = new JLabel("Vai trò:");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roleLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(roleLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(roleComboBox, gbc);
        
        contentPanel.add(rightPanel, BorderLayout.CENTER);
        
        cardPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Panel nút ngang, căn giữa
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0)); // 16px spacing
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0)); // Margin top 24px
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);
        
        cardPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Căn giữa cardPanel trong mainPanel
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.gridx = 0; mainGbc.gridy = 0; mainGbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(cardPanel, mainGbc);
        
        add(mainPanel);
    }
    
    private void setupEventListeners() {
        registerButton.addActionListener(e -> performRegister());
        cancelButton.addActionListener(e -> dispose());
        
        // Preview button
        previewButton.addActionListener(e -> {
            String avatarUrl = avatarField.getText().trim();
            loadAvatarPreview(avatarPreview, avatarUrl);
        });
        
        // Enter key để đăng ký
        KeyAdapter enterKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performRegister();
                }
            }
        };
        
        usernameField.addKeyListener(enterKeyListener);
        passwordField.addKeyListener(enterKeyListener);
        confirmPasswordField.addKeyListener(enterKeyListener);
        fullNameField.addKeyListener(enterKeyListener);
        emailField.addKeyListener(enterKeyListener);
        avatarField.addKeyListener(enterKeyListener);
    }
    
    private void loadAvatarPreview(JLabel previewLabel, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            previewLabel.setIcon(null);
            previewLabel.setText("Không có ảnh");
            return;
        }
        
        // Use ImageCache for loading with caching (fetch larger image for better sharpness)
        ImageIcon cachedIcon = ImageCache.getInstance().getImage(avatarUrl, 320, 320, new ImageCache.ImageLoadCallback() {
            @Override
            public void onImageLoaded(ImageIcon icon) {
                SwingUtilities.invokeLater(() -> {
                    if (icon != null) {
                        previewLabel.setIcon(icon);
                        previewLabel.setText("");
                    } else {
                        previewLabel.setIcon(null);
                        previewLabel.setText("Lỗi tải ảnh");
                    }
                });
            }
        });
        
        if (cachedIcon != null) {
            // Already cached, use immediately
            previewLabel.setIcon(cachedIcon);
            previewLabel.setText("");
        } else {
            // Loading from cache or internet
            previewLabel.setIcon(null);
            previewLabel.setText("Đang tải...");
        }
    }
    
    private void performRegister() {
        // Lấy dữ liệu từ form
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String avatar = avatarField.getText().trim();
        // Chỉ cho phép đăng ký vai trò USER
        String role = "USER";
        
        // Validate dữ liệu
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            usernameField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mật khẩu!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            passwordField.requestFocus();
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            confirmPasswordField.requestFocus();
            return;
        }
        
        if (fullName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập họ và tên!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            fullNameField.requestFocus();
            return;
        }
        
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập email!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            emailField.requestFocus();
            return;
        }
        
        // Validate email format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, "Email không hợp lệ!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            emailField.requestFocus();
            return;
        }
        
        // Vô hiệu hóa nút trong khi xử lý
        registerButton.setEnabled(false);
        registerButton.setText("Đang đăng ký...");
        
        // Thực hiện đăng ký trong thread riêng
        new Thread(() -> {
            try {
                Request request = new Request("ADMIN_CREATE_USER");
                request.put("username", username);
                request.put("password", password);
                request.put("fullName", fullName);
                request.put("email", email);
                request.put("avatar", avatar.isEmpty() ? null : avatar);
                request.put("role", role);
                
                Response response = networkClient.send(request);
                
                SwingUtilities.invokeLater(() -> {
                    registerButton.setEnabled(true);
                    registerButton.setText("Đăng ký");
                    
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(this, 
                            "Đăng ký tài khoản thành công!", 
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            response.getMessage(), 
                            "Lỗi đăng ký", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    registerButton.setEnabled(true);
                    registerButton.setText("Đăng ký");
                    
                    JOptionPane.showMessageDialog(this, 
                        "Lỗi kết nối: " + e.getMessage(), 
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField(20);
        field.setPreferredSize(new Dimension(250, 40));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setBackground(Color.WHITE);
        
        // Placeholder effect
        field.setText(placeholder);
        field.setForeground(new Color(156, 163, 175));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(new Color(31, 41, 55));
                }
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(45, 156, 219), 2, true),
                    BorderFactory.createEmptyBorder(7, 11, 7, 11)
                ));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(156, 163, 175));
                }
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        
        return field;
    }
    
    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField(20);
        field.setPreferredSize(new Dimension(250, 40));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setBackground(Color.WHITE);
        field.setEchoChar('\u2022');
        
        // Placeholder effect
        field.setForeground(new Color(156, 163, 175));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setForeground(new Color(31, 41, 55));
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(45, 156, 219), 2, true),
                    BorderFactory.createEmptyBorder(7, 11, 7, 11)
                ));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        
        return field;
    }
    
    private JButton createStyledPrimaryButton(String text, Color accentColor) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(120, 42));
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(accentColor);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(accentColor.darker());
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(accentColor);
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setBackground(accentColor.darker().darker());
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                button.setBackground(accentColor.darker());
            }
        });
        
        return button;
    }
    
    private JButton createStyledSecondaryButton(String text, Color accentColor) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(120, 42));
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(Color.WHITE);
        button.setForeground(accentColor);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 1, true),
            BorderFactory.createEmptyBorder(7, 15, 7, 15)
        ));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(248, 250, 252));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(Color.WHITE);
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(241, 245, 249));
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(248, 250, 252));
            }
        });
        
        return button;
    }
    
    private void styleComboBox(JComboBox<String> combo) {
        combo.setPreferredSize(new Dimension(250, 40));
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }
}