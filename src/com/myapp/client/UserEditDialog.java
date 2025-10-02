package com.myapp.client;

import javax.swing.*;
import java.awt.*;

import com.myapp.common.*;

public class UserEditDialog extends JDialog {
    private NetworkClient networkClient;
    private User user;
    private boolean confirmed = false;
    
    // Form fields
    private JTextField usernameField;
    private JTextField fullNameField;
    private JTextField emailField;
    private JTextField avatarField;
    private JComboBox<String> roleCombo;
    private JComboBox<String> statusCombo;
    private JLabel avatarPreviewLabel;
    private JButton previewButton;
    
    public UserEditDialog(JFrame parent, String title, User user, NetworkClient client) {
        super(parent, title, true);
        this.user = user;
        this.networkClient = client;
        
        // Set modern look and feel
        setBackground(UIConstants.BG_PRIMARY);
        getContentPane().setBackground(UIConstants.BG_PRIMARY);
        
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        
        if (user != null) {
            populateFields();
        }
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        usernameField = new JTextField(20);
        fullNameField = new JTextField(20);
        emailField = new JTextField(20);
        avatarField = new JTextField(20);
        
        // Set modern fonts
        usernameField.setFont(UIConstants.FONT_BODY);
        fullNameField.setFont(UIConstants.FONT_BODY);
        emailField.setFont(UIConstants.FONT_BODY);
        avatarField.setFont(UIConstants.FONT_BODY);
        
        // Avatar preview components (no outer border, larger for higher quality)
        avatarPreviewLabel = new JLabel("Không có ảnh");
        avatarPreviewLabel.setPreferredSize(new Dimension(200, 200));
        avatarPreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarPreviewLabel.setFont(UIConstants.FONT_SMALL);
        avatarPreviewLabel.setBackground(UIConstants.SURFACE);
        avatarPreviewLabel.setOpaque(false); // let the dialog background show through
        
        previewButton = new JButton("Xem trước");
        previewButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        previewButton.setForeground(Color.WHITE);
        previewButton.setBackground(UIConstants.PRIMARY);
        previewButton.setFocusPainted(false);
        previewButton.setBorderPainted(false);
        previewButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
    roleCombo = new JComboBox<>(new String[]{"USER", "ADMIN"});
    statusCombo = new JComboBox<>(new String[]{"ACTIVE", "LOCKED"});
    roleCombo.setFont(UIConstants.FONT_BODY);
    statusCombo.setFont(UIConstants.FONT_BODY);
        
        // If editing existing user, disable username, role and status fields (cannot be changed via this dialog)
        if (user != null) {
            usernameField.setEnabled(false);
            roleCombo.setEnabled(false);
            statusCombo.setEnabled(false);
        }
    }
    
    private void layoutComponents() {
        setUndecorated(true); // Remove window decorations for custom look
        setSize(700, 500);
        setLocationRelativeTo(getParent());
        
        // Create main card container
        JPanel cardPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw shadow
                g2d.setColor(new Color(0, 0, 0, 30));
                g2d.fillRoundRect(10, 10, getWidth() - 10, getHeight() - 10, 20, 20);
                
                // Draw card background
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth() - 10, getHeight() - 10, 20, 20);
                
                g2d.dispose();
            }
        };
        cardPanel.setOpaque(false);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Header Panel với gradient
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                GradientPaint gradient = new GradientPaint(0, 0, new Color(52, 152, 219), getWidth(), 0, new Color(41, 128, 185));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                g2d.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setPreferredSize(new Dimension(0, 60));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        
        String title = user == null ? "Tạo người dùng mới" : "Cập nhật thông tin người dùng";
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // Close button
        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        headerPanel.add(closeButton, BorderLayout.EAST);
        
        cardPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
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
        
        avatarPreviewLabel.setPreferredSize(new Dimension(200, 200));
        avatarPreviewLabel.setHorizontalAlignment(JLabel.CENTER);
        avatarPreviewLabel.setText("👤");
        avatarPreviewLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
        avatarPreviewLabel.setOpaque(false);
        
    avatarContainer.add(avatarPreviewLabel);
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
        
        // Username
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Tên đăng nhập:");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        usernameLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(usernameLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(usernameField, gbc);
        
        // Full Name
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel fullNameLabel = new JLabel("Họ tên:");
        fullNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        fullNameLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(fullNameLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(fullNameField, gbc);
        
        // Email
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        emailLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(emailLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(emailField, gbc);
        
        // Avatar URL
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel avatarUrlLabel = new JLabel("Avatar URL:");
        avatarUrlLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        avatarUrlLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(avatarUrlLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(avatarField, gbc);
        
    // ... preview button moved to the left (under avatar) to match User edit layout
        
        // Role
        gbc.gridx = 0; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel roleLabel = new JLabel("Vai trò:");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roleLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(roleLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(roleCombo, gbc);
        
        // Status
        gbc.gridx = 0; gbc.gridy = 6; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel statusLabel = new JLabel("Trạng thái:");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(statusLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(statusCombo, gbc);
        
        contentPanel.add(rightPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton saveButton = new JButton("Lưu");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveButton.setForeground(Color.WHITE);
        saveButton.setBackground(UIConstants.SUCCESS);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setPreferredSize(new Dimension(120, 40));
        saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JButton cancelButton = new JButton("Hủy");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setBackground(new Color(149, 165, 166));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setPreferredSize(new Dimension(100, 40));
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        cardPanel.add(contentPanel, BorderLayout.CENTER);
        
        add(cardPanel);
        
        // Event handlers
        saveButton.addActionListener(e -> saveUser());
    }
    
    private void setupEventHandlers() {
        // Preview button
        previewButton.addActionListener(e -> {
            String avatarUrl = avatarField.getText().trim();
            loadAvatarPreview(avatarPreviewLabel, avatarUrl);
        });
        
        // Add tooltips
        usernameField.setToolTipText("Tên đăng nhập duy nhất của người dùng");
        fullNameField.setToolTipText("Họ và tên đầy đủ của người dùng");
        emailField.setToolTipText("Địa chỉ email hợp lệ");
        avatarField.setToolTipText("URL của ảnh đại diện");
        previewButton.setToolTipText("Xem trước ảnh đại diện");
        roleCombo.setToolTipText("Vai trò của người dùng trong hệ thống");
        statusCombo.setToolTipText("Trạng thái tài khoản");
    }
    
    private void populateFields() {
        usernameField.setText(user.getUsername());
        fullNameField.setText(user.getFullName());
        emailField.setText(user.getEmail());
        avatarField.setText(user.getAvatar() != null ? user.getAvatar() : "");
        roleCombo.setSelectedItem(user.getRole());
        statusCombo.setSelectedItem(user.getStatus());
        
        // Load avatar preview if available
        if (user.getAvatar() != null && !user.getAvatar().trim().isEmpty()) {
            loadAvatarPreview(avatarPreviewLabel, user.getAvatar());
        }
        
        // Don't populate password field for security
    }
    
    private void saveUser() {
        // Validate input
        String validation = validateInput();
        if (validation != null) {
            JOptionPane.showMessageDialog(this, validation, "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        SwingWorker<Response, Void> worker = new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                String action = user == null ? "ADMIN_CREATE_USER" : "ADMIN_EDIT_USER";
                Request request = new Request(action);

                if (user != null) {
                    request.put("id", String.valueOf(user.getId()));
                }

                request.put("username", usernameField.getText().trim());
                request.put("fullName", fullNameField.getText().trim());
                request.put("email", emailField.getText().trim());
                request.put("avatar", avatarField.getText().trim());

                // Only include role/status when creating a new user
                if (user == null) {
                    request.put("role", (String) roleCombo.getSelectedItem());
                    request.put("status", (String) statusCombo.getSelectedItem());
                }

                // Password removed, so no password sending

                Response response = networkClient.send(request);
                return response;
            }

            @Override
            protected void done() {
                try {
                    Response resp = get();
                    if (resp != null && resp.isSuccess()) {
                        // Invalidate any cached avatar for this URL so other views reload the updated image
                        String avatarUrl = avatarField.getText().trim();
                        if (!avatarUrl.isEmpty()) {
                            try {
                                ImageCache.getInstance().remove(avatarUrl);
                            } catch (Exception ex) {
                                // Non-fatal: log and continue
                                ex.printStackTrace();
                            }
                        }
                        confirmed = true;
                        dispose();
                    } else {
                        String msg = (resp != null) ? resp.getMessage() : "No response from server";
                        JOptionPane.showMessageDialog(UserEditDialog.this,
                            "Không thể lưu thông tin người dùng: " + msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(UserEditDialog.this,
                        "Lỗi khi lưu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private String validateInput() {
        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        
        // For new users, password is required - but since we removed password, just validate basic fields
        if (user == null) {
            return UIUtils.validateUserInput(username, "", fullName, email);
        } else {
            // For existing users
            if (username.isEmpty()) return "Tên đăng nhập không được để trống!";
            if (fullName.isEmpty()) return "Họ tên không được để trống!";
            if (email.isEmpty()) return "Email không được để trống!";
            
            // Validate email format
            if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                return "Email không đúng định dạng!";
            }
        }
        
        return null;
    }
    
    public boolean isConfirmed() {
        return confirmed;
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
}