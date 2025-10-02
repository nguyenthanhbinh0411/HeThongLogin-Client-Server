package com.myapp.client;

import com.myapp.common.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Giao diện người dùng với thiết kế hiện đại
 * Hiển thị thông tin cá nhân với các chức năng sửa thông tin và đổi mật khẩu
 */
public class UserFrame extends JFrame {

    private static final int AVATAR_DISPLAY_SIZE = 180;
    private static final int AVATAR_PREVIEW_SIZE = 480;
    private static final int AVATAR_FETCH_SIZE = 512;
    private static final int AVATAR_PREVIEW_FETCH_SIZE = 768;
    private static final Pattern IP_PATTERN = Pattern.compile("\\b\\d{1,3}(?:\\.\\d{1,3}){3}\\b");

    private User currentUser;
    private NetworkClient networkClient;
    
    // UI Components cho hiển thị thông tin
    private JLabel usernameLabel;
    private JLabel fullNameLabel;
    private JLabel emailLabel;
    private JLabel roleLabel;
    private JLabel avatarLabel;
    private ImageIcon currentAvatarIcon;
    
    public UserFrame(User user, NetworkClient client) {
        this.currentUser = user;
        this.networkClient = client;
        
        initializeComponents();
        setupLayout();
        setupEventListeners();
        loadUserData();
        
        setTitle("Bảng điều khiển người dùng - " + user.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 600); // Increased by 20%
        setLocationRelativeTo(null);
        setResizable(false);
    }
    
    /**
     * Khởi tạo các thành phần giao diện
     */
    private void initializeComponents() {
        // Khởi tạo các label để hiển thị thông tin người dùng
        usernameLabel = new JLabel();
        fullNameLabel = new JLabel();
        emailLabel = new JLabel();
        roleLabel = new JLabel();
        
        // Avatar label với khả năng phóng to
        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(AVATAR_DISPLAY_SIZE, AVATAR_DISPLAY_SIZE));
        avatarLabel.setBorder(BorderFactory.createEmptyBorder());
        avatarLabel.setHorizontalAlignment(JLabel.CENTER);
        avatarLabel.setText("👤");
        avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 84));
        avatarLabel.setOpaque(false);
        avatarLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarLabel.setToolTipText("Nhấn để xem ảnh ở độ phân giải cao");
        avatarLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    showAvatarPreviewDialog();
                }
            }
        });
        
        // Thiết lập font và màu sắc
        usernameLabel.setFont(UIConstants.FONT_BODY);
        fullNameLabel.setFont(UIConstants.FONT_BODY);
        emailLabel.setFont(UIConstants.FONT_BODY);
        roleLabel.setFont(UIConstants.FONT_BODY);
        
        usernameLabel.setForeground(UIConstants.PRIMARY);
        fullNameLabel.setForeground(UIConstants.TEXT_PRIMARY);
        emailLabel.setForeground(UIConstants.TEXT_PRIMARY);
        roleLabel.setForeground(UIConstants.TEXT_PRIMARY);
    }
    
    /**
     * Thiết lập bố cục giao diện
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(240, 248, 255)); // Light blue background
        
        // Create main card container
        JPanel cardPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw shadow
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fillRoundRect(8, 8, getWidth() - 8, getHeight() - 8, 16, 16);
                
                // Draw card background
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth() - 8, getHeight() - 8, 16, 16);
                
                g2d.dispose();
            }
        };
        cardPanel.setOpaque(false);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        // Header Panel với tiêu đề
        JPanel headerPanel = createHeaderPanel();
        cardPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Main Content Panel với thông tin tài khoản
        JPanel mainPanel = createMainPanel();
        cardPanel.add(mainPanel, BorderLayout.CENTER);
        
        // Button Panel với các nút chức năng
        JPanel buttonPanel = createButtonPanel();
        cardPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Center the card
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.weighty = 1;
        centerPanel.add(cardPanel, gbc);
        
        add(centerPanel, BorderLayout.CENTER);
    }
    
    /**
     * Tạo panel header với tiêu đề
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                GradientPaint gradient = new GradientPaint(0, 0, new Color(70, 130, 180), getWidth(), 0, new Color(100, 149, 237));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                g2d.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setPreferredSize(new Dimension(0, 60));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        // Title label
        JLabel titleLabel = new JLabel("Thông tin tài khoản");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        return headerPanel;
    }
    
    /**
     * Tạo panel nội dung chính với thông tin tài khoản
     */
    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 50, 30)); // Increased bottom padding by 20px
        
        // Left panel - Avatar
        JPanel avatarPanel = new JPanel(new GridBagLayout());
        avatarPanel.setBackground(Color.WHITE);
        avatarPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
        
        // Make avatar circular với kích thước lớn hơn
        avatarLabel.setPreferredSize(new Dimension(AVATAR_DISPLAY_SIZE, AVATAR_DISPLAY_SIZE));
        avatarLabel.setBorder(BorderFactory.createEmptyBorder());
        avatarLabel.setHorizontalAlignment(JLabel.CENTER);
        avatarLabel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.weighty = 1;
        avatarPanel.add(avatarLabel, gbc);
        
        // Right panel - Info grid
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(Color.WHITE);
        
        GridBagConstraints infoGbc = new GridBagConstraints();
        infoGbc.anchor = GridBagConstraints.WEST;
        infoGbc.insets = new Insets(10, 0, 10, 0);
        
        // Username
        infoGbc.gridx = 0; infoGbc.gridy = 0;
        infoGbc.insets = new Insets(10, 0, 10, 0);
        JLabel usernameTitleLabel = new JLabel("Tên đăng nhập:");
        usernameTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        usernameTitleLabel.setForeground(new Color(100, 100, 100));
        infoPanel.add(usernameTitleLabel, infoGbc);
        
        infoGbc.gridx = 1; 
        infoGbc.insets = new Insets(10, 15, 10, 0);
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameLabel.setForeground(new Color(50, 50, 50));
        infoPanel.add(usernameLabel, infoGbc);
        
        // Full name
        infoGbc.gridx = 0; infoGbc.gridy = 1; 
        infoGbc.insets = new Insets(10, 0, 10, 0);
        JLabel fullNameTitleLabel = new JLabel("Họ và tên:");
        fullNameTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        fullNameTitleLabel.setForeground(new Color(100, 100, 100));
        infoPanel.add(fullNameTitleLabel, infoGbc);
        
        infoGbc.gridx = 1; 
        infoGbc.insets = new Insets(10, 15, 10, 0);
        fullNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fullNameLabel.setForeground(new Color(50, 50, 50));
        infoPanel.add(fullNameLabel, infoGbc);
        
        // Email
        infoGbc.gridx = 0; infoGbc.gridy = 2; 
        infoGbc.insets = new Insets(10, 0, 10, 0);
        JLabel emailTitleLabel = new JLabel("Email:");
        emailTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        emailTitleLabel.setForeground(new Color(100, 100, 100));
        infoPanel.add(emailTitleLabel, infoGbc);
        
        infoGbc.gridx = 1; 
        infoGbc.insets = new Insets(10, 15, 10, 0);
        emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emailLabel.setForeground(new Color(50, 50, 50));
        infoPanel.add(emailLabel, infoGbc);
        
        // Role
        infoGbc.gridx = 0; infoGbc.gridy = 3; 
        infoGbc.insets = new Insets(10, 0, 10, 0);
        infoGbc.anchor = GridBagConstraints.NORTHWEST; // Ensure proper alignment
        JLabel roleTitleLabel = new JLabel("Vai trò:");
        roleTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roleTitleLabel.setForeground(new Color(100, 100, 100));
        infoPanel.add(roleTitleLabel, infoGbc);
        
        infoGbc.gridx = 1; 
        infoGbc.insets = new Insets(10, 15, 10, 0);
        infoGbc.anchor = GridBagConstraints.NORTHWEST; // Ensure proper alignment
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        roleLabel.setForeground(new Color(50, 50, 50));
        infoPanel.add(roleLabel, infoGbc);
        
        mainPanel.add(avatarPanel, BorderLayout.WEST);
        mainPanel.add(infoPanel, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * Tạo panel với các nút chức năng
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 25));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        
        // Nút Sửa thông tin
        JButton editInfoButton = createModernButton("Sửa thông tin", new Color(52, 152, 219), Color.WHITE);
        editInfoButton.addActionListener(e -> showEditProfileDialog());
        buttonPanel.add(editInfoButton);
        
        // Nút Đổi mật khẩu
        JButton changePasswordButton = createModernButton("Đổi mật khẩu", new Color(230, 126, 34), Color.WHITE);
        changePasswordButton.addActionListener(e -> showChangePasswordDialog());
        buttonPanel.add(changePasswordButton);
        
        // Nút Lịch sử đăng nhập
        JButton historyButton = createModernButton("Lịch sử đăng nhập", new Color(46, 204, 113), Color.WHITE);
        historyButton.addActionListener(e -> showLoginHistoryDialog());
        buttonPanel.add(historyButton);
        
        // Nút Đăng xuất
        JButton logoutButton = createModernButton("Đăng xuất", new Color(231, 76, 60), Color.WHITE);
        logoutButton.addActionListener(e -> logout());
        buttonPanel.add(logoutButton);
        
        return buttonPanel;
    }
    
    /**
     * Tạo text field hiện đại với styling
     */
    private JTextField createModernTextField(String text) {
        JTextField textField = new JTextField(text);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        textField.setBackground(new Color(250, 250, 250));
        textField.setPreferredSize(new Dimension(200, 35));
        return textField;
    }
    
    /**
     * Tạo password field hiện đại với styling
     */
    private JPasswordField createModernPasswordField() {
        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        passwordField.setBackground(new Color(250, 250, 250));
        passwordField.setPreferredSize(new Dimension(200, 35));
        return passwordField;
    }
    
    /**
     * Tạo nút hiện đại với hover effects
     */
    private JButton createModernButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2d.setColor(bgColor.darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(bgColor.brighter());
                } else {
                    g2d.setColor(bgColor);
                }
                
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2d.dispose();
                
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(130, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }
    
    /**
     * Tạo nút hiện đại với hover effects (overload)
     */
    private JButton createModernButton(String text, Color bgColor) {
        return createModernButton(text, bgColor, Color.WHITE);
    }
    
    /**
     * Thiết lập các sự kiện
     */
    private void setupEventListeners() {
        // Setup window closing event
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                logout();
            }
        });
    }
    
    /**
     * Tải dữ liệu người dùng và hiển thị trên giao diện
     */
    private void loadUserData() {
        // Hiển thị thông tin người dùng
        usernameLabel.setText(currentUser.getUsername());
        fullNameLabel.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "");
        emailLabel.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        roleLabel.setText("USER".equals(currentUser.getRole()) ? "NGƯỜI DÙNG" : "QUẢN TRỊ VIÊN");
        
        // Load avatar
        loadAvatar();
    }
    
    private void loadAvatar() {
        String avatarUrl = currentUser.getAvatar();
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            showDefaultAvatar();
            return;
        }

        avatarLabel.setIcon(null);
        avatarLabel.setText("Đang tải...");
    avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
    avatarLabel.setForeground(new Color(120, 125, 130));

    ImageIcon cachedIcon = ImageCache.getInstance().getImage(avatarUrl, AVATAR_FETCH_SIZE, AVATAR_FETCH_SIZE, new ImageCache.ImageLoadCallback() {
            @Override
            public void onImageLoaded(ImageIcon icon) {
                SwingUtilities.invokeLater(() -> {
                    if (icon != null) {
                        currentAvatarIcon = icon;
                        ImageIcon circularIcon = makeCircularImageIcon(icon, AVATAR_DISPLAY_SIZE);
                        avatarLabel.setIcon(circularIcon);
                        avatarLabel.setText("");
                    } else {
                        showDefaultAvatar();
                    }
                });
            }
        });

        if (cachedIcon != null) {
            currentAvatarIcon = cachedIcon;
            ImageIcon circularIcon = makeCircularImageIcon(cachedIcon, AVATAR_DISPLAY_SIZE);
            avatarLabel.setIcon(circularIcon);
            avatarLabel.setText("");
        }
    }

    private void showDefaultAvatar() {
        currentAvatarIcon = null;
        avatarLabel.setIcon(null);
        avatarLabel.setText("👤");
    avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 84));
        avatarLabel.setForeground(new Color(140, 150, 160));
    }
    
    /**
     * Utility to convert an ImageIcon into a circular ImageIcon with the given size.
     * This performs high-quality scaling and a circular mask to avoid jagged edges.
     */
    private ImageIcon makeCircularImageIcon(ImageIcon srcIcon, int size) {
        if (srcIcon == null || srcIcon.getImage() == null) return null;

        Image src = srcIcon.getImage();
        BufferedImage buff = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buff.createGraphics();
        
        // Enhanced rendering hints for maximum image quality
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Draw the scaled image centered and cover the area
        g2.setComposite(AlphaComposite.Src);
        g2.setColor(new Color(0,0,0,0));
        g2.fillRect(0, 0, size, size);

        // Scale while preserving aspect ratio
        int iw = src.getWidth(null);
        int ih = src.getHeight(null);
        if (iw <= 0 || ih <= 0) {
            g2.dispose();
            return new ImageIcon(buff);
        }

        double scale = Math.max((double) size / iw, (double) size / ih);
        int w = (int) Math.round(iw * scale);
        int h = (int) Math.round(ih * scale);

        int x = (size - w) / 2;
        int y = (size - h) / 2;

        g2.drawImage(src, x, y, w, h, null);

        // Apply circular mask with high-quality anti-aliasing
        g2.setComposite(AlphaComposite.DstIn);
        g2.setColor(Color.BLACK);
        g2.fillOval(0, 0, size, size);
        g2.dispose();

        return new ImageIcon(buff);
    }

    private void showAvatarPreviewDialog() {
        String avatarUrl = currentUser.getAvatar();
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bạn chưa thiết lập ảnh đại diện.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

    JDialog dialog = new JDialog(this, "Ảnh đại diện", true);
    dialog.setSize(620, 700);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);

        JPanel cardPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(new Color(0, 0, 0, 30));
                g2d.fillRoundRect(10, 10, getWidth() - 10, getHeight() - 10, 20, 20);

                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth() - 10, getHeight() - 10, 20, 20);

                g2d.dispose();
            }
        };
        cardPanel.setOpaque(false);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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

        JLabel titleLabel = new JLabel("Ảnh đại diện độ phân giải cao");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dialog.dispose());
        headerPanel.add(closeButton, BorderLayout.EAST);

        cardPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

    JLabel largeAvatar = new JLabel("Đang tải ảnh...", JLabel.CENTER);
    largeAvatar.setPreferredSize(new Dimension(AVATAR_PREVIEW_SIZE, AVATAR_PREVIEW_SIZE));
        largeAvatar.setFont(new Font("Segoe UI", Font.BOLD, 18));
        largeAvatar.setForeground(new Color(110, 115, 120));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(largeAvatar);
        contentPanel.add(centerPanel, BorderLayout.CENTER);

        JButton closeBtn = createModernButton("Đóng", new Color(149, 165, 166));
        closeBtn.setPreferredSize(new Dimension(110, 40));
        closeBtn.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeBtn);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        cardPanel.add(contentPanel, BorderLayout.CENTER);
        dialog.add(cardPanel);

    ImageIcon hiResIcon = ImageCache.getInstance().getImage(avatarUrl, AVATAR_PREVIEW_FETCH_SIZE, AVATAR_PREVIEW_FETCH_SIZE, new ImageCache.ImageLoadCallback() {
            @Override
            public void onImageLoaded(ImageIcon icon) {
                SwingUtilities.invokeLater(() -> {
                    if (icon != null) {
                        ImageIcon circular = makeCircularImageIcon(icon, AVATAR_PREVIEW_SIZE);
                        largeAvatar.setIcon(circular);
                        largeAvatar.setText("");
                    } else if (currentAvatarIcon != null) {
                        ImageIcon fallback = makeCircularImageIcon(currentAvatarIcon, AVATAR_PREVIEW_SIZE);
                        largeAvatar.setIcon(fallback);
                        largeAvatar.setText("");
                    } else {
                        largeAvatar.setText("Không thể tải ảnh");
                    }
                });
            }
        });

        if (hiResIcon != null) {
            ImageIcon circular = makeCircularImageIcon(hiResIcon, AVATAR_PREVIEW_SIZE);
            largeAvatar.setIcon(circular);
            largeAvatar.setText("");
        } else if (currentAvatarIcon != null) {
            ImageIcon fallback = makeCircularImageIcon(currentAvatarIcon, AVATAR_PREVIEW_SIZE);
            largeAvatar.setIcon(fallback);
            largeAvatar.setText("");
        }

        dialog.setVisible(true);
    }
    
    /**
     * Hiển thị dialog sửa thông tin cá nhân
     */
    private void showEditProfileDialog() {
        JDialog dialog = new JDialog(this, "Sửa thông tin cá nhân", true);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true); // Remove window decorations for custom look
        
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
        
        JLabel titleLabel = new JLabel("Sửa thông tin cá nhân");
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
        closeButton.addActionListener(e -> dialog.dispose());
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
        avatarContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 2, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
    JLabel avatarPreview = new JLabel();
    avatarPreview.setPreferredSize(new Dimension(150, 150));
        avatarPreview.setHorizontalAlignment(JLabel.CENTER);
        avatarPreview.setText("👤");
        avatarPreview.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        avatarPreview.setOpaque(false);
        
        avatarContainer.add(avatarPreview);
        avatarContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(avatarContainer);
        
        contentPanel.add(leftPanel, BorderLayout.WEST);
        
        // Right panel - Form fields
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(15, 10, 15, 10);
        
        // Full Name
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel fullNameLabel = new JLabel("Họ và tên:");
        fullNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        fullNameLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(fullNameLabel, gbc);
        
        JTextField fullNameField = createModernTextField(currentUser.getFullName());
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(fullNameField, gbc);
        
        // Email
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        emailLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(emailLabel, gbc);
        
        JTextField emailField = createModernTextField(currentUser.getEmail());
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(emailField, gbc);
        
        // Avatar URL
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel avatarUrlLabel = new JLabel("Avatar URL:");
        avatarUrlLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        avatarUrlLabel.setForeground(new Color(70, 70, 70));
        rightPanel.add(avatarUrlLabel, gbc);
        
        JTextField avatarField = createModernTextField(currentUser.getAvatar() != null ? currentUser.getAvatar() : "");
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rightPanel.add(avatarField, gbc);
        
        // Preview button
        JButton previewBtn = createModernButton("Xem trước", new Color(52, 152, 219));
        previewBtn.setPreferredSize(new Dimension(100, 35));
        gbc.gridx = 1; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        rightPanel.add(previewBtn, gbc);
        
        contentPanel.add(rightPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton saveButton = createModernButton("Lưu thay đổi", new Color(46, 204, 113));
        saveButton.setPreferredSize(new Dimension(120, 40));
        
        JButton cancelButton = createModernButton("Hủy", new Color(149, 165, 166));
        cancelButton.setPreferredSize(new Dimension(100, 40));
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        cardPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Load initial avatar
        loadAvatarPreview(avatarPreview, currentUser.getAvatar());
        
        // Preview button action
        previewBtn.addActionListener(e -> loadAvatarPreview(avatarPreview, avatarField.getText().trim()));
        
        // Save button action
        saveButton.addActionListener(e -> {
            try {
                Request request = new Request("UPDATE_PROFILE");
                request.put("fullName", fullNameField.getText().trim());
                request.put("email", emailField.getText().trim());
                request.put("avatar", avatarField.getText().trim());
                
                Response response = networkClient.send(request);
                if (response.isSuccess()) {
                    // Update current user info
                    currentUser.setFullName(fullNameField.getText().trim());
                    currentUser.setEmail(emailField.getText().trim());
                    currentUser.setAvatar(avatarField.getText().trim());
                    loadUserData(); // Refresh display
                    
                    JOptionPane.showMessageDialog(dialog, "Cập nhật thông tin thành công!");
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Lỗi: " + response.getMessage());
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi kết nối: " + ex.getMessage());
            }
        });
        
        dialog.add(cardPanel);
        dialog.setVisible(true);
    }
    
    private void loadAvatarPreview(JLabel previewLabel, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            previewLabel.setIcon(null);
            previewLabel.setText("👤");
            return;
        }
        
    final int previewDisplaySize = 150;

        // Use ImageCache for loading with caching and high quality scaling
        ImageIcon cachedIcon = ImageCache.getInstance().getImage(avatarUrl, AVATAR_FETCH_SIZE, AVATAR_FETCH_SIZE, new ImageCache.ImageLoadCallback() {
            @Override
            public void onImageLoaded(ImageIcon icon) {
                SwingUtilities.invokeLater(() -> {
                    if (icon != null) {
                        ImageIcon circular = makeCircularImageIcon(icon, previewDisplaySize);
                        previewLabel.setIcon(circular);
                        previewLabel.setText("");
                    } else {
                        previewLabel.setIcon(null);
                        previewLabel.setText("❌");
                    }
                });
            }
        });

        if (cachedIcon != null) {
            ImageIcon circular = makeCircularImageIcon(cachedIcon, previewDisplaySize);
            previewLabel.setIcon(circular);
            previewLabel.setText("");
        } else {
            previewLabel.setIcon(null);
            previewLabel.setText("🔄");
        }
    }
    
    /**
     * Hiển thị dialog đổi mật khẩu
     */
    private void showChangePasswordDialog() {
        JDialog dialog = new JDialog(this, "Đổi mật khẩu", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        
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
                
                GradientPaint gradient = new GradientPaint(0, 0, new Color(230, 126, 34), getWidth(), 0, new Color(211, 84, 0));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                g2d.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setPreferredSize(new Dimension(0, 60));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        
        JLabel titleLabel = new JLabel("Đổi mật khẩu");
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
        closeButton.addActionListener(e -> dialog.dispose());
        headerPanel.add(closeButton, BorderLayout.EAST);
        
        cardPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Content panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(15, 10, 15, 10);
        
        // Old password
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel oldPasswordLabel = new JLabel("Mật khẩu cũ:");
        oldPasswordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        oldPasswordLabel.setForeground(new Color(70, 70, 70));
        contentPanel.add(oldPasswordLabel, gbc);
        
        JPasswordField oldPasswordField = createModernPasswordField();
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        contentPanel.add(oldPasswordField, gbc);
        
        // New password
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel newPasswordLabel = new JLabel("Mật khẩu mới:");
        newPasswordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        newPasswordLabel.setForeground(new Color(70, 70, 70));
        contentPanel.add(newPasswordLabel, gbc);
        
        JPasswordField newPasswordField = createModernPasswordField();
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        contentPanel.add(newPasswordField, gbc);
        
        // Confirm password
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel confirmPasswordLabel = new JLabel("Xác nhận mật khẩu:");
        confirmPasswordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        confirmPasswordLabel.setForeground(new Color(70, 70, 70));
        contentPanel.add(confirmPasswordLabel, gbc);
        
        JPasswordField confirmPasswordField = createModernPasswordField();
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        contentPanel.add(confirmPasswordField, gbc);
        
        cardPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton changeButton = createModernButton("Đổi mật khẩu", new Color(230, 126, 34));
        changeButton.setPreferredSize(new Dimension(130, 40));
        
        JButton cancelButton = createModernButton("Hủy", new Color(149, 165, 166));
        cancelButton.setPreferredSize(new Dimension(100, 40));
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(changeButton);
        
        cardPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Change password action
        changeButton.addActionListener(e -> {
            String oldPassword = new String(oldPasswordField.getPassword());
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            
            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập đầy đủ thông tin!");
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(dialog, "Mật khẩu xác nhận không khớp!");
                return;
            }
            
            if (newPassword.length() < 6) {
                JOptionPane.showMessageDialog(dialog, "Mật khẩu mới phải có ít nhất 6 ký tự!");
                return;
            }
            
            try {
                Request request = new Request("CHANGE_PASSWORD");
                request.put("oldPassword", oldPassword);
                request.put("newPassword", newPassword);
                
                Response response = networkClient.send(request);
                if (response.isSuccess()) {
                    JOptionPane.showMessageDialog(dialog, "Đổi mật khẩu thành công!");
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Lỗi: " + response.getMessage());
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi kết nối: " + ex.getMessage());
            }
        });
        
        dialog.add(cardPanel);
        dialog.setVisible(true);
    }
    
    /**
     * Hiển thị dialog lịch sử đăng nhập
     */
    private void showLoginHistoryDialog() {
        JDialog dialog = new JDialog(this, "Lịch sử đăng nhập", true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        
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
                
                GradientPaint gradient = new GradientPaint(0, 0, new Color(46, 204, 113), getWidth(), 0, new Color(39, 174, 96));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                g2d.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setPreferredSize(new Dimension(0, 60));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        
        JLabel titleLabel = new JLabel("Lịch sử đăng nhập");
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
        closeButton.addActionListener(e -> dialog.dispose());
        headerPanel.add(closeButton, BorderLayout.EAST);
        
        cardPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create modern table
        String[] columnNames = {"Thời gian", "Hoạt động", "Chi tiết", "Địa chỉ IP"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable historyTable = new JTable(tableModel);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        historyTable.setRowHeight(30);
        historyTable.setGridColor(new Color(230, 230, 230));
        historyTable.setShowGrid(true);
        historyTable.getTableHeader().setReorderingAllowed(false);
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        historyTable.getTableHeader().setBackground(new Color(245, 245, 245));
        historyTable.getTableHeader().setForeground(new Color(70, 70, 70));
        historyTable.getTableHeader().setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Set column widths
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Thời gian
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Hoạt động
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(300); // Chi tiết
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(120); // IP
        
        // Modern table styling
        historyTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                    
                    if (isSelected) {
                        label.setBackground(new Color(52, 152, 219, 50));
                        label.setForeground(Color.BLACK);
                    } else {
                        label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 250, 250));
                        label.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Footer panel with close button
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        JButton closeBtn = createModernButton("Đóng", new Color(149, 165, 166));
        closeBtn.setPreferredSize(new Dimension(100, 40));
        closeBtn.addActionListener(e -> dialog.dispose());
        footerPanel.add(closeBtn);
        
        contentPanel.add(footerPanel, BorderLayout.SOUTH);
        cardPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Load data in background
        new Thread(() -> {
            try {
                Request request = new Request("GET_USER_HISTORY");
                request.put("username", currentUser.getUsername());
                Response response = networkClient.send(request);
                
                SwingUtilities.invokeLater(() -> {
                    if (response.isSuccess()) {
                        String historyData = response.getData().get("history");
                        if (historyData != null && !historyData.trim().isEmpty()) {
                            String[] entries = historyData.split("\n");
                            for (String rawEntry : entries) {
                                if (rawEntry == null) continue;
                                String entry = rawEntry.trim();
                                if (entry.isEmpty()) continue;

                                String[] parts = entry.split("\\|");
                                if (parts.length < 5) {
                                    continue;
                                }

                                String type = parts[0];
                                if (!"AUDIT".equalsIgnoreCase(type)) {
                                    continue;
                                }

                                String time = parts[1] != null ? parts[1].trim() : "";
                                String activityRaw = parts[2] != null ? parts[2].trim() : "";
                                String details = parts[3] != null ? parts[3].trim() : "";

                                String activityLabel = activityRaw;
                                String normalizedActivity = activityRaw != null ? activityRaw.replace(" ", "_").toUpperCase() : "";
                                if (normalizedActivity.contains("LOGIN_SUCCESS")) activityLabel = "Đăng nhập thành công";
                                else if (normalizedActivity.contains("LOGIN_FAILED") || normalizedActivity.contains("LOGIN_FAILURE")) activityLabel = "Đăng nhập thất bại";
                                else if (normalizedActivity.contains("LOGOUT")) activityLabel = "Đăng xuất";
                                else if (normalizedActivity.contains("PROFILE_UPDATE") || normalizedActivity.contains("EDIT_USER") || normalizedActivity.contains("EDIT")) activityLabel = "Cập nhật thông tin";
                                else if (normalizedActivity.contains("PASSWORD_CHANGE")) activityLabel = "Đổi mật khẩu";

                                String ip = "N/A";
                                Matcher matcher = IP_PATTERN.matcher(details);
                                if (matcher.find()) {
                                    ip = matcher.group();
                                }

                                tableModel.addRow(new Object[]{time, activityLabel, details, ip});
                            }
                        }
                        
                        if (tableModel.getRowCount() == 0) {
                            tableModel.addRow(new Object[]{"N/A", "Chưa có dữ liệu", "N/A", "N/A"});
                        }
                    } else {
                        tableModel.addRow(new Object[]{"N/A", "Không thể tải dữ liệu", "N/A", "N/A"});
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    tableModel.addRow(new Object[]{"N/A", "Lỗi kết nối", "N/A", "N/A"});
                });
            }
        }).start();
        
        dialog.add(cardPanel);
        dialog.setVisible(true);
    }
    
    /**
     * Đăng xuất khỏi hệ thống
     */
    private void logout() {
        int option = JOptionPane.showConfirmDialog(this, 
            "Bạn có chắc chắn muốn đăng xuất?", 
            "Xác nhận đăng xuất", 
            JOptionPane.YES_NO_OPTION);
        
        if (option == JOptionPane.YES_OPTION) {
            try {
                // Gửi yêu cầu đăng xuất tới server
                Request request = new Request("PING"); // Server doesn't have LOGOUT action
                networkClient.send(request);
                
                // Đóng kết nối và quay về màn hình đăng nhập
                networkClient.close();
                dispose();
                
                // Mở lại giao diện đăng nhập
                SwingUtilities.invokeLater(() -> {
                    new LoginFrame().setVisible(true);
                });
                
            } catch (Exception e) {
                // Vẫn đóng cửa sổ và mở login frame ngay cả khi có lỗi
                dispose();
                SwingUtilities.invokeLater(() -> {
                    new LoginFrame().setVisible(true);
                });
            }
        }
    }
}