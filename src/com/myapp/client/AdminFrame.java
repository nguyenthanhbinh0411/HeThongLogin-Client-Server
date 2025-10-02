package com.myapp.client;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Ellipse2D;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.myapp.common.*;

public class AdminFrame extends JFrame {
    // Core components
    private User currentUser;
    private NetworkClient networkClient;

    private static final int TOP_BAR_AVATAR_DISPLAY_SIZE = 32;
    private static final int TOP_BAR_AVATAR_FETCH_SIZE = 256;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]{4,32}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern IP_PATTERN = Pattern.compile("\\b\\d{1,3}(?:\\.\\d{1,3}){3}\\b");
    
    // Main layout components
    private JPanel sideNavPanel;
    private JPanel topBarPanel;
    private JPanel mainContentPanel;
    private CardLayout cardLayout;

    
    // Dashboard components
    private JPanel dashboardPanel;
    private JLabel totalUsersLabel, onlineUsersLabel, lockedUsersLabel, newUsersLabel;
    
    // User management components
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextField globalSearchField;
    // Update-panel filters
    private JTextField updateSearchField;
    private JComboBox<String> updateRoleFilter;
    private JComboBox<String> updateStatusFilter;
    private JComboBox<String> statusFilter;
    private JComboBox<String> roleFilter;
    private JButton createUserBtn, editUserBtn, deleteUserBtn, refreshBtn;
    
    // Pagination variables
    private int userTablePage = 1;
    private int updateUsersPage = 1;
    private final int USERS_PER_PAGE = 10;
    private int totalUserPages = 1;
    private int totalUpdateUserPages = 1;
    // Modern action cell renderer with primary action and dropdown menu
    private class ActionCellRenderer extends JPanel implements TableCellRenderer {
        private JButton primaryBtn;
        private JButton menuBtn;
        
        public ActionCellRenderer() {
            // Use GridBagLayout so we can center the inner button panel vertically and horizontally
            setLayout(new GridBagLayout());
            setOpaque(true);

            // Primary action button (Lock/Unlock)
            primaryBtn = new JButton();
            primaryBtn.setPreferredSize(new Dimension(92, 34));
            primaryBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            primaryBtn.setFocusPainted(false);
            primaryBtn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            primaryBtn.setAlignmentY(Component.CENTER_ALIGNMENT);

            // History button
            menuBtn = new JButton("Lịch sử");
            menuBtn.setPreferredSize(new Dimension(78, 34));
            menuBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            menuBtn.setFocusPainted(false);
            menuBtn.setBackground(new Color(243, 244, 246));
            menuBtn.setForeground(new Color(51, 65, 85));
            menuBtn.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));
            menuBtn.setAlignmentY(Component.CENTER_ALIGNMENT);

            // Inner panel holds buttons horizontally; it will be centered by GridBagLayout
            JPanel inner = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            inner.setOpaque(false);
            inner.add(primaryBtn);
            inner.add(menuBtn);

            add(inner); // centered by GridBagLayout
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            if (value instanceof User) {
                User user = (User) value;
                boolean isLocked = "LOCKED".equals(user.getStatus());
                
                if (isLocked) {
                    primaryBtn.setText("Mở khóa");
                    primaryBtn.setBackground(new Color(59, 130, 246)); // Blue
                    primaryBtn.setForeground(Color.WHITE);
                } else {
                    primaryBtn.setText("Khóa");
                    primaryBtn.setBackground(new Color(239, 68, 68)); // Red
                    primaryBtn.setForeground(Color.WHITE);
                }
            }
            
            // Background styling and ensure buttons fit without clipping
            Color bgColor = isSelected ? table.getSelectionBackground() : table.getBackground();
            setBackground(bgColor);
            
            return this;
        }
    }
    
    private JLabel statusLabel;
    private JProgressBar progressBar;
    
    // Data storage
    private List<User> users = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private Map<String, Boolean> onlineUsers = new ConcurrentHashMap<>();
    // Audit cache / summary (kept in memory for quick access if needed)
    private volatile int recentAuditCount = 0;
    private volatile String recentAuditSummary = "";
    // Activity log pagination
    private int activityLogPage = 1;
    private final int ACTIVITY_LOGS_PER_PAGE = 30;
    
    // Navigation buttons map for styling
    private Map<String, JButton> navButtons = new HashMap<>();
    
    // Table columns
    private final String[] columnNames = {
        "ID", "Avatar", "Tài khoản", "Email", "Vai trò",
        "Trạng thái", "Online", "Ngày tạo", "Thao tác"
    };
    
    // Heartbeat timer
    private javax.swing.Timer heartbeatTimer;
    
    public AdminFrame(User user, NetworkClient client) {
        this.currentUser = user;
        this.networkClient = client;
        
        initializeUI();
        loadUsers();
        startHeartbeat();
    }
    
    private void initializeUI() {
        setTitle("Admin Dashboard - " + currentUser.getFullName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        // Set Look and Feel
        try {
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            Method installMethod = flatLafClass.getMethod("install");
            installMethod.invoke(null);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        SwingUtilities.updateComponentTreeUI(this);
        
        // Initialize components
        initializeComponents();
        
        // Create main layout with 3 regions
        createMainLayout();
        
        // Set colors and styling
        getContentPane().setBackground(new Color(245, 247, 250));
        
        // Add window state listener for restore functionality
        addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                // When window is restored from maximized state, set to a smaller size
                if ((e.getOldState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH && 
                    (e.getNewState() & JFrame.MAXIMIZED_BOTH) != JFrame.MAXIMIZED_BOTH) {
                    // Set to a smaller size when restored
                    setSize(1200, 800);
                    setLocationRelativeTo(null); // Center on screen
                }
            }
        });
    }
    
    private void initializeComponents() {
        // Initialize CardLayout for main content
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(new Color(245, 247, 250));
        
        // Create dashboard
        createDashboard();
        
        // Create user management panel (existing table functionality)
        createUserManagementPanel();
        
        // Create activity log panel
        JPanel activityLogPanel = createActivityLogPanel();
        
        // Add panels to CardLayout
        mainContentPanel.add(dashboardPanel, "DASHBOARD");
        mainContentPanel.add(createUserTablePanel(), "USERS");
        mainContentPanel.add(createUpdateUsersPanel(), "UPDATE_USERS");
        mainContentPanel.add(createCreateUserPanel(), "CREATE_USER");
        mainContentPanel.add(activityLogPanel, "LOGS");
    }

    // Circular avatar label class
    private static class CircularAvatarLabel extends JLabel {
        public CircularAvatarLabel() {
            super();
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            
            // Enhanced rendering hints for maximum image quality and smooth edges
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            
            // Get the icon to draw
            Icon icon = getIcon();
            if (icon != null) {
                int width = getWidth();
                int height = getHeight();
                
                // Create smooth circular clip
                Ellipse2D.Float circle = new Ellipse2D.Float(0.5f, 0.5f, width - 1, height - 1);
                g2.setClip(circle);
                
                // Calculate position to center the icon
                int iconWidth = icon.getIconWidth();
                int iconHeight = icon.getIconHeight();
                int x = (width - iconWidth) / 2;
                int y = (height - iconHeight) / 2;
                
                // Draw the icon directly for better quality control
                icon.paintIcon(this, g2, x, y);
            }
            
            g2.setClip(null);
            int width = getWidth();
            int height = getHeight();
            float borderWidth = Math.max(1.5f, Math.min(width, height) / 24f);
            g2.setStroke(new BasicStroke(borderWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 255, 255, 235));
            double inset = borderWidth / 2.0;
            g2.draw(new Ellipse2D.Double(inset, inset, width - borderWidth, height - borderWidth));

            if (icon == null && getText() != null && !getText().isEmpty()) {
                g2.dispose();
                super.paintComponent(g);
                return;
            }
            
            g2.dispose();
        }
    }

    private ImageIcon createHighQualityCircularIcon(ImageIcon srcIcon, int size) {
        if (srcIcon == null || srcIcon.getImage() == null) return null;

        int iw = srcIcon.getIconWidth();
        int ih = srcIcon.getIconHeight();
        if (iw <= 0 || ih <= 0) return null;

        BufferedImage original = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g0 = original.createGraphics();
        g0.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g0.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g0.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g0.drawImage(srcIcon.getImage(), 0, 0, null);
        g0.dispose();

        double scale = Math.max((double) size / iw, (double) size / ih);
        int targetW = Math.max(size, (int) Math.ceil(iw * scale));
        int targetH = Math.max(size, (int) Math.ceil(ih * scale));

        BufferedImage scaled = downscaleImageWithQuality(original, targetW, targetH);

        BufferedImage canvas = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int x = (size - scaled.getWidth()) / 2;
        int y = (size - scaled.getHeight()) / 2;
        g2.setComposite(AlphaComposite.Src);
        g2.drawImage(scaled, x, y, null);

        g2.setComposite(AlphaComposite.DstIn);
        g2.setColor(Color.BLACK);
        g2.fillOval(0, 0, size, size);
        g2.dispose();

        return new ImageIcon(canvas);
    }

    private BufferedImage downscaleImageWithQuality(BufferedImage src, int targetWidth, int targetHeight) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage current = src;

        while (width > targetWidth || height > targetHeight) {
            int nextWidth = width > targetWidth ? Math.max(targetWidth, width / 2) : width;
            int nextHeight = height > targetHeight ? Math.max(targetHeight, height / 2) : height;

            BufferedImage tmp = new BufferedImage(nextWidth, nextHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(current, 0, 0, nextWidth, nextHeight, null);
            g2.dispose();

            current = tmp;
            width = nextWidth;
            height = nextHeight;
        }

        if (width != targetWidth || height != targetHeight) {
            BufferedImage tmp = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(current, 0, 0, targetWidth, targetHeight, null);
            g2.dispose();
            current = tmp;
        }

        return current;
    }

    private void createMainLayout() {
        setLayout(new BorderLayout());
        
        // Create side navigation
        sideNavPanel = createSideNavigation();
        add(sideNavPanel, BorderLayout.WEST);
        
        // Main content area with top bar and content
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Create top bar
        topBarPanel = createTopBar();
        rightPanel.add(topBarPanel, BorderLayout.NORTH);
        
        // Add main content
        rightPanel.add(mainContentPanel, BorderLayout.CENTER);
        
        add(rightPanel, BorderLayout.CENTER);
    }
    
    private JPanel createSideNavigation() {
        JPanel sideNav = new JPanel();
        sideNav.setLayout(new BorderLayout());
        sideNav.setBackground(Color.WHITE);
        sideNav.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 225, 230)));
        sideNav.setPreferredSize(new Dimension(250, 0));
        
        // Header with logo/title
        JPanel navHeader = new JPanel(new FlowLayout(FlowLayout.LEFT));
        navHeader.setBackground(Color.WHITE);
        navHeader.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        
        JLabel logoLabel = new JLabel("Admin Panel");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoLabel.setForeground(new Color(32, 41, 56));
        navHeader.add(logoLabel);
        
        sideNav.add(navHeader, BorderLayout.NORTH);
        
        // Navigation menu
        JPanel navMenu = new JPanel();
        navMenu.setLayout(new BoxLayout(navMenu, BoxLayout.Y_AXIS));
        navMenu.setBackground(Color.WHITE);
        navMenu.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        
        // Overview section
        addNavSection(navMenu, "TỔNG QUAN");
        addNavItem(navMenu, "Dashboard", "DASHBOARD", true);
        
        // User management section
        addNavSection(navMenu, "QUẢN LÝ NGƯỜI DÙNG");
        addNavItem(navMenu, "Danh sách người dùng", "USERS", false);
        addNavItem(navMenu, "Cập nhật thông tin", "UPDATE_USERS", false);
        addNavItem(navMenu, "Tạo tài khoản", "CREATE_USER", false);
        
        // Activity section
        addNavSection(navMenu, "HOẠT ĐỘNG");
        addNavItem(navMenu, "Lịch sử hoạt động", "LOGS", false);
        
        JScrollPane scrollPane = new JScrollPane(navMenu);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sideNav.add(scrollPane, BorderLayout.CENTER);
        
        // Set initial selection
        updateNavSelection("DASHBOARD");
        
        return sideNav;
    }
    
    private void addNavSection(JPanel parent, String title) {
        JLabel sectionLabel = new JLabel(title);
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sectionLabel.setForeground(new Color(120, 130, 140));
        sectionLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 8, 0));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(sectionLabel);
    }
    
    private void addNavItem(JPanel parent, String text, String action, boolean selected) {
        JButton navButton = new JButton(text);
        navButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        navButton.setHorizontalAlignment(SwingConstants.LEFT);
        navButton.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        navButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        navButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        navButton.setFocusPainted(false);
        
        // Store button in map
        navButtons.put(action, navButton);
        
        // Set initial style
        updateNavButtonStyle(navButton, selected);
        
        navButton.addActionListener(e -> switchToPanel(action));
        parent.add(navButton);
        parent.add(Box.createVerticalStrut(4));
    }
    
    private void switchToPanel(String panelName) {
        cardLayout.show(mainContentPanel, panelName);
        
        // Update navigation selection
        updateNavSelection(panelName);
        
        // Load data based on panel
        if ("DASHBOARD".equals(panelName)) {
            // Refresh dashboard data including activity feed and pie chart
            updateDashboardKPIs();
            Component[] components = dashboardPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JPanel) {
                    refreshActivityFeed((JPanel) comp);
                    // Also refresh pie chart
                    refreshPieChart((JPanel) comp);
                }
            }
        } else if ("LOGS".equals(panelName)) {
            loadActivityLogs();
        } else if ("UPDATE_USERS".equals(panelName)) {
            loadUpdateUsersData();
        } else if ("CREATE_USER".equals(panelName)) {
            // No data loading needed for create user form
        }
    }
    
    private void updateNavSelection(String selectedPanel) {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton btn = entry.getValue();
            boolean isSelected = entry.getKey().equals(selectedPanel);
            updateNavButtonStyle(btn, isSelected);
        }
    }
    
    private void updateNavButtonStyle(JButton button, boolean selected) {
        if (selected) {
            button.setBackground(new Color(33, 123, 244, 50)); // Light blue background for selected
            button.setForeground(new Color(33, 123, 244)); // Blue text
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 123, 244), 1),
                BorderFactory.createEmptyBorder(11, 15, 11, 15)
            ));
        } else {
            button.setBackground(Color.WHITE); // White background for unselected
            button.setForeground(new Color(32, 41, 56)); // Dark text
            button.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16)); // Reset border
        }
    }
    
    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 225, 230)),
            BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        topBar.setPreferredSize(new Dimension(0, 64));
        
        // Left: Title and breadcrumb
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("QUẢN LÝ NGƯỜI DÙNG");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(32, 41, 56));
        leftPanel.add(titleLabel);
        
        topBar.add(leftPanel, BorderLayout.WEST);
        
        // Right: User profile and actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        
        // User avatar and name
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        userPanel.setOpaque(false);
        
        // Avatar label
        CircularAvatarLabel userAvatar = new CircularAvatarLabel();
        Dimension topBarAvatarDimension = new Dimension(TOP_BAR_AVATAR_DISPLAY_SIZE, TOP_BAR_AVATAR_DISPLAY_SIZE);
        userAvatar.setPreferredSize(topBarAvatarDimension);
        userAvatar.setMinimumSize(topBarAvatarDimension);
        userAvatar.setMaximumSize(topBarAvatarDimension);
        userAvatar.setHorizontalAlignment(SwingConstants.CENTER);
        if (currentUser.getAvatar() != null && !currentUser.getAvatar().trim().isEmpty()) {
            ImageIcon avatarIcon = ImageCache.getInstance().getImage(currentUser.getAvatar(), TOP_BAR_AVATAR_FETCH_SIZE, TOP_BAR_AVATAR_FETCH_SIZE, new ImageCache.ImageLoadCallback() {
                @Override
                public void onImageLoaded(ImageIcon icon) {
                    SwingUtilities.invokeLater(() -> {
                        if (icon != null) {
                            ImageIcon processed = createHighQualityCircularIcon(icon, TOP_BAR_AVATAR_DISPLAY_SIZE);
                            if (processed != null) {
                                userAvatar.setIcon(processed);
                                userAvatar.setText("");
                            } else {
                                userAvatar.setIcon(null);
                                userAvatar.setText("👤");
                            }
                        } else {
                            userAvatar.setIcon(null);
                            userAvatar.setText("👤");
                        }
                    });
                }
            });
            if (avatarIcon != null) {
                ImageIcon processed = createHighQualityCircularIcon(avatarIcon, TOP_BAR_AVATAR_DISPLAY_SIZE);
                if (processed != null) {
                    userAvatar.setIcon(processed);
                    userAvatar.setText("");
                } else {
                    userAvatar.setIcon(null);
                    userAvatar.setText("👤");
                }
            } else {
                userAvatar.setText("👤");
            }
        } else {
            userAvatar.setText("👤");
        }
        userPanel.add(userAvatar);
        
        JLabel userName = new JLabel(currentUser.getFullName());
        userName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userName.setForeground(new Color(32, 41, 56));
        userPanel.add(userName);
        
        // Logout button
        JButton logoutBtn = new JButton("Đăng xuất");
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logoutBtn.setBackground(new Color(231, 76, 60));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> logout());
        userPanel.add(logoutBtn);
        
        rightPanel.add(userPanel);
        topBar.add(rightPanel, BorderLayout.EAST);
        
        return topBar;
    }
    
    private void showUserMenu() {
        // Create popup menu for user actions
        JPopupMenu userMenu = new JPopupMenu();
        
        JMenuItem profileItem = new JMenuItem("Hồ sơ");
        profileItem.addActionListener(e -> {
            // TODO: Open profile dialog
        });
        userMenu.add(profileItem);
        
        JMenuItem settingsItem = new JMenuItem("Cài đặt");
        settingsItem.addActionListener(e -> {
            // TODO: Open settings
        });
        userMenu.add(settingsItem);
        
        userMenu.addSeparator();
        
        JMenuItem logoutItem = new JMenuItem("Đăng xuất");
        logoutItem.addActionListener(e -> logout());
        userMenu.add(logoutItem);
        
        // Show menu relative to the top bar
        userMenu.show(topBarPanel, topBarPanel.getWidth() - 150, topBarPanel.getHeight());
    }
    
    private void createDashboard() {
        dashboardPanel = new JPanel(new BorderLayout());
        dashboardPanel.setBackground(new Color(245, 247, 250));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        
        // Header with breadcrumb and quick actions
        JPanel dashboardHeader = new JPanel(new BorderLayout());
        dashboardHeader.setOpaque(false);
        
        JLabel breadcrumb = new JLabel("Dashboard / Tổng quan");
        breadcrumb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        breadcrumb.setForeground(new Color(120, 130, 140));
        dashboardHeader.add(breadcrumb, BorderLayout.WEST);
        
        dashboardPanel.add(dashboardHeader, BorderLayout.NORTH);
        
        // Main dashboard content
        JPanel dashboardContent = new JPanel(new BorderLayout());
        dashboardContent.setOpaque(false);
        dashboardContent.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
        
        // KPI Cards Row
        JPanel kpiPanel = createKPIPanel();
        dashboardContent.add(kpiPanel, BorderLayout.NORTH);
        
        // Charts and activity feed
        JPanel chartsPanel = createChartsPanel();
        dashboardContent.add(chartsPanel, BorderLayout.CENTER);
        
        dashboardPanel.add(dashboardContent, BorderLayout.CENTER);
    }
    
    private JPanel createKPIPanel() {
        JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiPanel.setOpaque(false);
        kpiPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
        
        // Initialize KPI labels
        totalUsersLabel = new JLabel("0");
        onlineUsersLabel = new JLabel("0");
        lockedUsersLabel = new JLabel("0");
        newUsersLabel = new JLabel("0");
        
        // Create KPI cards
        kpiPanel.add(createKPICard("", "Tổng số người dùng", totalUsersLabel, new Color(33, 123, 244)));
        kpiPanel.add(createKPICard("", "Đang trực tuyến", onlineUsersLabel, new Color(46, 204, 113)));
        kpiPanel.add(createKPICard("", "Bị khóa", lockedUsersLabel, new Color(231, 76, 60)));
        kpiPanel.add(createKPICard("", "Người mới (7 ngày)", newUsersLabel, new Color(155, 89, 182)));
        
        return kpiPanel;
    }
    
    private JPanel createKPICard(String icon, String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Title only
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(new Color(120, 130, 140));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        card.add(headerPanel, BorderLayout.NORTH);
        
        // Value
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valueLabel.setForeground(accentColor);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        card.add(valueLabel, BorderLayout.CENTER);
        
        return card;
    }
    
    private JPanel createChartsPanel() {
        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsPanel.setOpaque(false);
        
        // Activity chart with modern card styling
        JPanel activityChart = new JPanel(new BorderLayout());
        activityChart.setBackground(Color.WHITE);
        activityChart.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true), // Soft border
            BorderFactory.createEmptyBorder(24, 24, 24, 24) // Larger padding
        ));
        
        // Add subtle shadow effect (simulated with border)
        activityChart.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 2, new Color(0, 0, 0, 20)), // Shadow simulation
            activityChart.getBorder()
        ));
        
        JLabel chartTitle = new JLabel("Hoạt động người dùng (7 ngày qua)");
        chartTitle.setFont(new Font("Segoe UI", Font.BOLD, 18)); // Larger font
        chartTitle.setForeground(new Color(32, 41, 56));
        chartTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0)); // Space below title
        activityChart.add(chartTitle, BorderLayout.NORTH);
        
        // Create pie chart panel
        PieChartPanel pieChartPanel = createStatusDistributionChart();
        activityChart.add(pieChartPanel, BorderLayout.CENTER);
        
        chartsPanel.add(activityChart);
        
        // Recent activity feed
        JPanel activityFeed = createActivityFeed();
        chartsPanel.add(activityFeed);
        
        return chartsPanel;
    }
    
    private JPanel createActivityFeed() {
        JPanel feedPanel = new JPanel(new BorderLayout());
        feedPanel.setBackground(Color.WHITE);
        feedPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel feedTitle = new JLabel("Hoạt động gần đây");
        feedTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        feedTitle.setForeground(new Color(32, 41, 56));

        JButton refreshActivityBtn = new JButton("Làm mới");
        refreshActivityBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshActivityBtn.setForeground(Color.WHITE);
        refreshActivityBtn.setBackground(new Color(59, 130, 246));
        refreshActivityBtn.setFocusPainted(false);
        refreshActivityBtn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        refreshActivityBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(feedTitle, BorderLayout.WEST);
        headerPanel.add(refreshActivityBtn, BorderLayout.EAST);
        feedPanel.add(headerPanel, BorderLayout.NORTH);
        
    // Activity list
    final JPanel activityList = new JPanel();
    // đặt tên để có thể tìm chính xác khi refresh
    activityList.setName("recentActivityList");
        activityList.setLayout(new BoxLayout(activityList, BoxLayout.Y_AXIS));
        activityList.setBackground(Color.WHITE);
        activityList.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        
        // Load real activity data
        refreshActivityBtn.addActionListener(e -> loadRecentActivity(activityList, refreshActivityBtn));
        loadRecentActivity(activityList);
        
    JScrollPane scrollPane = new JScrollPane(activityList);
    // also mark scroll pane for easier detection
    scrollPane.setName("recentActivityScroll");
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        feedPanel.add(scrollPane, BorderLayout.CENTER);
        
        return feedPanel;
    }
    
    private void loadRecentActivity(JPanel activityList) {
        loadRecentActivity(activityList, null);
    }

    private void loadRecentActivity(JPanel activityList, JButton triggerButton) {
        if (triggerButton != null) {
            triggerButton.setEnabled(false);
            triggerButton.setText("Đang tải...");
        }

        // Clear existing items
        activityList.removeAll();
        activityList.revalidate();
        activityList.repaint();
        
        // Load users data first, then show activity
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // First load users data
                Request request = new Request("ADMIN_LIST_USERS");
                request.put("requestedBy", currentUser.getUsername());
                
                Response response = networkClient.send(request);
                if (response != null && response.isSuccess()) {
                    String usersStr = response.getData().get("users");
                    if (usersStr != null && !usersStr.trim().isEmpty()) {
                        List<User> tempUsers = new ArrayList<>();
                        String[] userEntries = usersStr.split(";");
                        for (String entry : userEntries) {
                            if (!entry.trim().isEmpty()) {
                                String[] parts = entry.split(",", -1);
                                if (parts.length >= 10) {
                                    User user = new User();
                                    try {
                                        user.setId(Integer.parseInt(parts[0]));
                                        user.setUsername(parts[1]);
                                        user.setFullName(parts[2]);
                                        user.setEmail(parts[3]);
                                        user.setAvatar(parts[4]);
                                        user.setRole(parts[5]);
                                        user.setStatus(parts[6]);

                                        String onlineStatus = parts[7];
                                        String lastLogin = parts[8];
                                        String createdAt = parts[9];

                                        user.setLastLogin(lastLogin != null && !lastLogin.trim().isEmpty() ? lastLogin : "Chưa đăng nhập");
                                        user.setCreatedAt(createdAt != null ? createdAt : "");

                                        onlineUsers.put(user.getUsername(), "ONLINE".equalsIgnoreCase(onlineStatus));
                                        tempUsers.add(user);
                                    } catch (NumberFormatException e) {
                                        System.err.println("Error parsing user data: " + entry);
                                    }
                                }
                            }
                        }
                        
                                // Show recent activity from users data
                                SwingUtilities.invokeLater(() -> {
                                    for (User user : tempUsers) {
                                        boolean isOnline = onlineUsers.getOrDefault(user.getUsername(), false);
                                        String timeInfo = "";
                                        if (!isOnline && user.getLastLogin() != null && !"Chưa đăng nhập".equals(user.getLastLogin())) {
                                            timeInfo = " - " + formatLastLoginTime(user.getLastLogin());
                                        }

                                        // Use UserChip component for activity entry
                                        UserChip chip = new UserChip(user.getFullName(), user.getUsername(), user.getAvatar(), isOnline, timeInfo);
                                        activityList.add(chip);
                                        activityList.add(Box.createRigidArea(new Dimension(0, 4)));
                                    }

                                    // After user-based items, also fetch recent audit logs (to show edits / cập nhật)
                                    SwingWorker<Void, Void> auditWorker = new SwingWorker<Void, Void>() {
                                        @Override
                                        protected Void doInBackground() throws Exception {
                                            try {
                                                Request auditReq = new Request("GET_AUDITS");
                                                Response auditResp = networkClient.send(auditReq);
                                                if (auditResp != null && auditResp.isSuccess()) {
                                                    String auditsData = auditResp.getData().get("audits");
                                                    if (auditsData != null && !auditsData.trim().isEmpty()) {
                                                        String[] records = auditsData.split("\n");
                                                        // Compute a small summary and keep it in memory; do NOT update UI here
                                                        int count = 0;
                                                        StringBuilder sb = new StringBuilder();
                                                        for (int i = Math.max(0, records.length - 20); i < records.length; i++) {
                                                            String rec = records[i];
                                                            if (rec == null || rec.trim().isEmpty()) continue;
                                                            String[] p = rec.split("\\|", -1);
                                                            if (p.length >= 5) {
                                                                String username = p[1] != null ? p[1] : "";
                                                                String action = p[2] != null ? p[2] : "";
                                                                String createdAt = p[4] != null ? p[4] : "";
                                                                count++;
                                                                if (sb.length() > 0) sb.append("; ");
                                                                sb.append(username).append(":").append(translateActivity(action.toUpperCase()));
                                                            }
                                                        }
                                                        recentAuditCount = count;
                                                        recentAuditSummary = sb.toString();
                                                        // optional: log for debugging
                                                        System.out.println("GET_AUDITS summary: count=" + recentAuditCount + " sample=" + (recentAuditSummary.length() > 120 ? recentAuditSummary.substring(0, 120) + "..." : recentAuditSummary));
                                                    } else {
                                                        recentAuditCount = 0;
                                                        recentAuditSummary = "";
                                                    }
                                                }
                                            } catch (Exception e) {
                                                // keep summaries empty on error
                                                recentAuditCount = 0;
                                                recentAuditSummary = "";
                                            }
                                            return null;
                                        }
                                    };
                                    auditWorker.execute();
                                });
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                SwingUtilities.invokeLater(() -> {
                    if (triggerButton != null) {
                        triggerButton.setEnabled(true);
                        triggerButton.setText("Làm mới");
                    }
                    if (activityList.getComponentCount() == 0) {
                        JLabel emptyLabel = new JLabel("Chưa có hoạt động nào.");
                        emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                        emptyLabel.setForeground(new Color(107, 114, 128));
                        emptyLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
                        activityList.add(emptyLabel);
                    }
                    activityList.revalidate();
                    activityList.repaint();
                });
            }
        };
        worker.execute();
    }
    
    private void addActivityItem(JPanel parent, String activity, String status) {
        // kept for compatibility but no longer used; add simple fallback rendering
        JPanel item = new JPanel(new BorderLayout());
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        item.setOpaque(false);
        item.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        
        JLabel activityLabel = new JLabel(activity);
        activityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        activityLabel.setForeground(new Color(32, 41, 56));
        item.add(activityLabel, BorderLayout.CENTER);
        
        JLabel statusLabel = new JLabel(status);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        if ("Đang trực tuyến".equals(status)) {
            statusLabel.setForeground(new Color(46, 204, 113));
        } else {
            statusLabel.setForeground(new Color(231, 76, 60));
        }
        item.add(statusLabel, BorderLayout.EAST);
        
        parent.add(item);
    }

    // Chuyển mã hoạt động từ audit thành chuỗi hiển thị tiếng Việt ngắn gọn
    private String translateActivity(String code) {
        if (code == null) return "Hoạt động";
        code = code.toUpperCase();
        if (code.contains("LOGIN_SUCCESS")) return "Đăng nhập thành công";
        if (code.contains("LOGIN_FAILED") || code.contains("LOGIN_FAILURE")) return "Đăng nhập thất bại";
        if (code.contains("LOGOUT")) return "Đăng xuất";
        if (code.contains("PROFILE_UPDATE") || code.contains("EDIT_USER") || code.contains("EDIT")) return "Cập nhật thông tin";
        if (code.contains("PASSWORD_CHANGE")) return "Đổi mật khẩu";
        return code; // fallback
    }
    
    private PieChartPanel createStatusDistributionChart() {
        // Create custom pie chart
        PieChartPanel pieChart = new PieChartPanel();
        
        // Load chart data
        loadStatusDistributionData(pieChart);
        
        return pieChart;
    }
    
    private void loadStatusDistributionData(PieChartPanel pieChart) {
        SwingWorker<Map<String, Integer>, Void> worker = new SwingWorker<Map<String, Integer>, Void>() {
            @Override
            protected Map<String, Integer> doInBackground() throws Exception {
                Map<String, Integer> statusCounts = new HashMap<>();
                
                // Get users data
                Request request = new Request("ADMIN_LIST_USERS");
                request.put("requestedBy", currentUser.getUsername());
                
                Response response = networkClient.send(request);
                if (response != null && response.isSuccess()) {
                    String usersStr = response.getData().get("users");
                    if (usersStr != null && !usersStr.trim().isEmpty()) {
                        String[] userEntries = usersStr.split(";");
                        for (String entry : userEntries) {
                            if (!entry.trim().isEmpty()) {
                                String[] parts = entry.split(",", -1);
                                if (parts.length >= 7) { // Ensure we have role and status
                                    String role = parts[5]; // Role is at index 5
                                    String status = parts[6]; // Status is at index 6
                                    
                                    // Combine role and status for categorization
                                    String key = role.toLowerCase() + "_" + status.toLowerCase();
                                    statusCounts.put(key, statusCounts.getOrDefault(key, 0) + 1);
                                }
                            }
                        }
                    }
                }
                
                return statusCounts;
            }
            
            @Override
            protected void done() {
                try {
                    Map<String, Integer> statusCounts = get();
                    Map<String, Color> statusColors = new HashMap<>();
                    // Define colors for the 4 categories
                    statusColors.put("admin_active", new Color(46, 204, 113)); // Green for admin active
                    statusColors.put("user_active", new Color(52, 152, 219)); // Blue for user active
                    statusColors.put("admin_locked", new Color(231, 76, 60)); // Red for admin locked
                    statusColors.put("user_locked", new Color(155, 89, 182)); // Purple for user locked
                    
                    pieChart.setData(statusCounts, statusColors);
                    pieChart.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    
    private void loadActivityLogs() {
        // Find the log panel and its table model
        Component[] components = mainContentPanel.getComponents();
        DefaultTableModel logTableModel = null;
        
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                Object tableModel = panel.getClientProperty("tableModel");
                if (tableModel instanceof DefaultTableModel) {
                    logTableModel = (DefaultTableModel) tableModel;
                    break;
                }
            }
        }
        
        if (logTableModel == null) return;

        final DefaultTableModel finalLogTableModel = logTableModel;

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // We'll collect all rows first in memory, then update the model on EDT
                List<Object[]> allRows = new ArrayList<>();

                // Get audit logs from server
                Request request = new Request("GET_AUDITS");
                Response response = networkClient.send(request);

                if (response != null && response.isSuccess()) {
                    String auditsData = response.getData().get("audits");
                    if (auditsData != null && !auditsData.trim().isEmpty()) {
                        String[] auditRecords = auditsData.split("\n");

                        for (String record : auditRecords) {
                            if (!record.trim().isEmpty()) {
                                String[] parts = record.split("\\|", -1);
                                if (parts.length >= 5) {
                                    // Server format for audits: id | username | action | details | createdAt
                                    String createdAt = parts[4] != null ? parts[4] : "";
                                    String username = parts[1] != null ? parts[1] : "System";
                                    String action = parts[2] != null ? parts[2] : "";
                                    String details = parts[3] != null ? parts[3] : "";

                                    String actionVi = action;
                                    if ("LOGIN_SUCCESS".equalsIgnoreCase(action) || action.toLowerCase().contains("login_success") || action.toLowerCase().contains("đăng nhập thành công")) {
                                        actionVi = "Đăng nhập thành công";
                                    } else if ("LOGIN_FAILED".equalsIgnoreCase(action) || action.toLowerCase().contains("login_failed") || action.toLowerCase().contains("đăng nhập thất bại")) {
                                        actionVi = "Đăng nhập thất bại";
                                    } else if ("LOGOUT".equalsIgnoreCase(action) || action.toLowerCase().contains("logout") || action.toLowerCase().contains("đăng xuất")) {
                                        actionVi = "Đăng xuất";
                                    } else if ("PROFILE_UPDATE".equalsIgnoreCase(action) || action.toLowerCase().contains("profile_update") || action.toLowerCase().contains("cập nhật")) {
                                        actionVi = "Cập nhật thông tin";
                                    } else if ("PASSWORD_CHANGE".equalsIgnoreCase(action) || action.toLowerCase().contains("password_change") || action.toLowerCase().contains("đổi mật khẩu")) {
                                        actionVi = "Đổi mật khẩu";
                                    }

                                    Object[] row = new Object[] { createdAt, username, actionVi, details };
                                    allRows.add(row);
                                }
                            }
                        }
                    }
                }

                // Get login attempts
                Request loginLogsRequest = new Request("GET_ALL_LOGIN_LOGS");
                Response loginLogsResponse = networkClient.send(loginLogsRequest);

                if (loginLogsResponse != null && loginLogsResponse.isSuccess()) {
                    String loginLogsData = loginLogsResponse.getData().get("loginLogs");
                    if (loginLogsData != null && !loginLogsData.trim().isEmpty()) {
                        String[] loginRecords = loginLogsData.split("\n");

                        for (String record : loginRecords) {
                            if (!record.trim().isEmpty()) {
                                String[] parts = record.split("\\|", -1);
                                // Expected: LOGIN_ATTEMPT|time|username|description|ip|result
                                if (parts.length >= 6 && "LOGIN_ATTEMPT".equalsIgnoreCase(parts[0])) {
                                    String time = parts[1] != null ? parts[1] : "";
                                    String uname = parts[2] != null ? parts[2] : "";
                                    String desc = parts[3] != null ? parts[3] : "";
                                    String actionVi = "Đăng nhập";
                                    if (desc != null && desc.toLowerCase().contains("thất bại")) actionVi = "Đăng nhập thất bại";
                                    else if (desc != null && desc.toLowerCase().contains("thành công")) actionVi = "Đăng nhập thành công";

                                    Object[] row = new Object[] { time, uname, actionVi, desc };
                                    allRows.add(row);
                                }
                            }
                        }
                    }
                }

                // Sort rows newest-first: assume DB timestamps are ISO-like so lexicographic works; otherwise parse
                allRows.sort((a, b) -> {
                    String ta = a[0] != null ? a[0].toString() : "";
                    String tb = b[0] != null ? b[0].toString() : "";
                    // descending
                    return tb.compareTo(ta);
                });

                final List<Object[]> rowsToShow = new ArrayList<>();
                int total = allRows.size();
                int pages = Math.max(1, (int) Math.ceil((double) total / ACTIVITY_LOGS_PER_PAGE));
                if (activityLogPage < 1) activityLogPage = 1;
                if (activityLogPage > pages) activityLogPage = pages;

                int start = (activityLogPage - 1) * ACTIVITY_LOGS_PER_PAGE;
                int end = Math.min(total, start + ACTIVITY_LOGS_PER_PAGE);

                for (int i = start; i < end; i++) rowsToShow.add(allRows.get(i));

                final int currentPage = activityLogPage;
                final int totalPages = pages;

                SwingUtilities.invokeLater(() -> {
                    finalLogTableModel.setRowCount(0);
                    for (Object[] r : rowsToShow) {
                        finalLogTableModel.addRow(r);
                    }

                    // Update page label and prev/next enable state if available
                    Component[] comps = mainContentPanel.getComponents();
                    for (Component comp : comps) {
                        if (comp instanceof JPanel) {
                            JPanel p = (JPanel) comp;
                            Object pm = p.getClientProperty("pageLabel");
                            if (pm instanceof JLabel) {
                                JLabel pl = (JLabel) pm;
                                pl.setText("Trang " + currentPage + " / " + totalPages);
                            }

                            Object prev = p.getClientProperty("prevPageBtn");
                            if (prev instanceof JButton) ((JButton) prev).setEnabled(currentPage > 1);
                            Object next = p.getClientProperty("nextPageBtn");
                            if (next instanceof JButton) ((JButton) next).setEnabled(currentPage < totalPages);
                        }
                    }
                });

                return null;
            }

            @Override
            protected void done() {
                // no-op
            }
        };

        worker.execute();
    }
    
    private void createUserManagementPanel() {
        // This will be implemented to wrap the existing table functionality
    }
    
    private JPanel createCreateUserPanel() {
        JPanel createPanel = new JPanel(new BorderLayout());
        createPanel.setBackground(new Color(245, 247, 250)); // #f5f7fa
        createPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        
        // Title
        JLabel titleLabel = new JLabel("Tạo tài khoản mới");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28)); // Increased size
        titleLabel.setForeground(new Color(34, 34, 34)); // #222222
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        createPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Main content panel - Card
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(Color.WHITE); // #ffffff
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1), // Soft border
            BorderFactory.createEmptyBorder(32, 32, 32, 32) // Wide padding
        ));
        cardPanel.setPreferredSize(new Dimension(800, 600)); // Fixed width ~800px
        cardPanel.setMaximumSize(new Dimension(800, 600));
        
        // Center the card
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerWrapper.setOpaque(false);
        centerWrapper.add(cardPanel);
        createPanel.add(centerWrapper, BorderLayout.CENTER);
        
        // Left panel - Avatar preview (30% width)
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 24)); // Margin right
        
        // Avatar preview with modern styling
        JLabel avatarPreview = new JLabel();
        avatarPreview.setPreferredSize(new Dimension(112, 112)); // 96-120px
        avatarPreview.setMaximumSize(new Dimension(112, 112));
        avatarPreview.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1), // #e5e7eb
            BorderFactory.createEmptyBorder(8, 8, 8, 8) // Padding
        ));
        avatarPreview.setHorizontalAlignment(JLabel.CENTER);
        avatarPreview.setText("👤");
        avatarPreview.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        avatarPreview.setOpaque(true);
        avatarPreview.setBackground(new Color(243, 244, 246)); // #f3f4f6
        avatarPreview.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(avatarPreview);
        leftPanel.add(Box.createVerticalStrut(12));
        
        // Description text
        JLabel descLabel = new JLabel("Ảnh đại diện");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(new Color(107, 114, 128)); // #6b7280
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(descLabel);
        
        cardPanel.add(leftPanel, BorderLayout.WEST);
        
        // Right panel - Form fields (70% width)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(18, 0, 18, 0); // Vertical spacing 18px
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Username field
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Tên đăng nhập:");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameLabel.setForeground(new Color(75, 85, 99)); // #4b5563
        formPanel.add(usernameLabel, gbc);
        gbc.gridx = 1; gbc.insets = new Insets(18, 12, 18, 0); // Horizontal spacing 12px
        JTextField usernameField = new JTextField(20);
        styleInputField(usernameField);
        formPanel.add(usernameField, gbc);
        
        // Password field
        gbc.gridx = 0; gbc.gridy = 1; gbc.insets = new Insets(18, 0, 18, 0);
        JLabel passwordLabel = new JLabel("Mật khẩu:");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordLabel.setForeground(new Color(75, 85, 99));
        formPanel.add(passwordLabel, gbc);
        gbc.gridx = 1; gbc.insets = new Insets(18, 12, 18, 0);
        JPasswordField passwordField = new JPasswordField(20);
        styleInputField(passwordField);
        formPanel.add(passwordField, gbc);
        
        // Full name field
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel fullNameLabel = new JLabel("Họ tên:");
        fullNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fullNameLabel.setForeground(new Color(75, 85, 99));
        formPanel.add(fullNameLabel, gbc);
        gbc.gridx = 1;
        JTextField fullNameField = new JTextField(20);
        styleInputField(fullNameField);
        formPanel.add(fullNameField, gbc);
        
        // Email field
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emailLabel.setForeground(new Color(75, 85, 99));
        formPanel.add(emailLabel, gbc);
        gbc.gridx = 1;
        JTextField emailField = new JTextField(20);
        styleInputField(emailField);
        formPanel.add(emailField, gbc);
        
        // Avatar field
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel avatarLabel = new JLabel("Avatar URL:");
        avatarLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        avatarLabel.setForeground(new Color(75, 85, 99));
        formPanel.add(avatarLabel, gbc);
        gbc.gridx = 1;
        JTextField avatarField = new JTextField(20);
        styleInputField(avatarField);
        avatarField.setPreferredSize(new Dimension(250, 40)); // Smaller width
        JPanel avatarInputPanel = new JPanel(new BorderLayout(8, 0));
        avatarInputPanel.setOpaque(false);
        avatarInputPanel.add(avatarField, BorderLayout.CENTER);
        JButton previewBtn = new JButton("Xem trước");
        styleSecondaryButton(previewBtn);
        previewBtn.setPreferredSize(new Dimension(100, 40));
        previewBtn.addActionListener(e -> loadAvatarPreview(avatarPreview, avatarField.getText().trim()));
        avatarInputPanel.add(previewBtn, BorderLayout.EAST);
        formPanel.add(avatarInputPanel, gbc);
        
        // Role field
        gbc.gridx = 0; gbc.gridy = 5;
        JLabel roleLabel = new JLabel("Vai trò:");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        roleLabel.setForeground(new Color(75, 85, 99));
        formPanel.add(roleLabel, gbc);
        gbc.gridx = 1;
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"USER", "ADMIN"});
        styleComboBox(roleCombo);
        formPanel.add(roleCombo, gbc);
        
        // Buttons
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.insets = new Insets(24, 0, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0)); // Spacing 16px
        buttonPanel.setOpaque(false);
        
        final Runnable resetFormFields = () -> {
            usernameField.setText("");
            passwordField.setText("");
            fullNameField.setText("");
            emailField.setText("");
            avatarField.setText("");
            avatarPreview.setIcon(null);
            avatarPreview.setText("👤");
            roleCombo.setSelectedIndex(0);
        };

        JButton createBtn = new JButton("Tạo tài khoản");
        stylePrimaryButton(createBtn);
        createBtn.addActionListener(e -> {
            String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
            String password = new String(passwordField.getPassword());
            String fullName = fullNameField.getText() != null ? fullNameField.getText().trim() : "";
            String email = emailField.getText() != null ? emailField.getText().trim() : "";
            String avatar = avatarField.getText() != null ? avatarField.getText().trim() : "";
            String role = (String) roleCombo.getSelectedItem();

            List<String> validationErrors = validateCreateUserInputs(username, password, fullName, email, avatar, role);
            if (!validationErrors.isEmpty()) {
                String message = "Vui lòng kiểm tra lại thông tin:\n• " + String.join("\n• ", validationErrors);
                JOptionPane.showMessageDialog(AdminFrame.this, message, "Thông tin không hợp lệ", JOptionPane.ERROR_MESSAGE);
                return;
            }

            createUserFromForm(username, password, fullName, email, avatar, role, () -> {
                resetFormFields.run();
                switchToPanel("USERS");
            });
        });
        
        JButton clearBtn = new JButton("Xóa");
        styleSecondaryButton(clearBtn);
        clearBtn.addActionListener(e -> resetFormFields.run());
        
        buttonPanel.add(createBtn);
        buttonPanel.add(clearBtn);
        formPanel.add(buttonPanel, gbc);
        
        cardPanel.add(formPanel, BorderLayout.CENTER);
        
        return createPanel;
    }
    
    private List<String> validateCreateUserInputs(String username, String password, String fullName, String email, String avatar, String role) {
        List<String> errors = new ArrayList<>();

        String normalizedUsername = username != null ? username.trim() : "";
        if (normalizedUsername.isEmpty()) {
            errors.add("Tên đăng nhập không được để trống.");
        } else if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            errors.add("Tên đăng nhập phải từ 4-32 ký tự và chỉ bao gồm chữ cái, số, dấu chấm, gạch ngang hoặc gạch dưới.");
        }

        String passwordValue = password != null ? password : "";
        if (passwordValue.trim().isEmpty()) {
            errors.add("Mật khẩu không được để trống.");
        } else if (!PASSWORD_PATTERN.matcher(passwordValue).matches()) {
            errors.add("Mật khẩu phải tối thiểu 8 ký tự và chứa ít nhất một chữ cái và một chữ số.");
        }

        String normalizedFullName = fullName != null ? fullName.trim() : "";
        if (normalizedFullName.isEmpty()) {
            errors.add("Họ tên không được để trống.");
        } else if (normalizedFullName.length() < 2) {
            errors.add("Họ tên phải có ít nhất 2 ký tự.");
        }

        String normalizedEmail = email != null ? email.trim() : "";
        if (normalizedEmail.isEmpty()) {
            errors.add("Email không được để trống.");
        } else if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            errors.add("Email không đúng định dạng.");
        }

        String normalizedAvatar = avatar != null ? avatar.trim() : "";
        if (!normalizedAvatar.isEmpty()) {
            if (normalizedAvatar.length() > 512) {
                errors.add("Đường dẫn ảnh đại diện quá dài (tối đa 512 ký tự).");
            } else {
                try {
                    URL url = new URL(normalizedAvatar);
                    String protocol = url.getProtocol();
                    if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                        errors.add("Ảnh đại diện phải sử dụng đường dẫn HTTP hoặc HTTPS.");
                    }
                } catch (MalformedURLException ex) {
                    errors.add("Ảnh đại diện phải là một URL hợp lệ.");
                }
            }
        }

        String normalizedRole = role != null ? role.trim().toUpperCase(Locale.ROOT) : "";
        if (normalizedRole.isEmpty()) {
            errors.add("Vui lòng chọn vai trò người dùng.");
        } else if (!"USER".equals(normalizedRole) && !"ADMIN".equals(normalizedRole)) {
            errors.add("Vai trò không hợp lệ.");
        }

        return errors;
    }

    private void createUserFromForm(String username, String password, String fullName, String email, String avatar, String role, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Request request = new Request("ADMIN_CREATE_USER");
                String normalizedUsername = username != null ? username.trim() : "";
                String normalizedFullName = fullName != null ? fullName.trim() : "";
                String normalizedEmail = email != null ? email.trim() : "";
                String normalizedAvatar = avatar != null ? avatar.trim() : "";
                String normalizedRole = role != null ? role.trim().toUpperCase(Locale.ROOT) : "USER";

                request.put("username", normalizedUsername);
                request.put("password", password != null ? password : "");
                request.put("fullName", normalizedFullName);
                request.put("email", normalizedEmail);
                request.put("avatar", normalizedAvatar.isEmpty() ? null : normalizedAvatar);
                request.put("role", normalizedRole);
                request.put("requestedBy", currentUser.getUsername());
                
                Response response = networkClient.send(request);
                
                SwingUtilities.invokeLater(() -> {
                    if (response != null && response.isSuccess()) {
                        JOptionPane.showMessageDialog(AdminFrame.this, 
                            "Tạo tài khoản thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                        // Refresh users data
                        loadUsers();
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Lỗi kết nối";
                        JOptionPane.showMessageDialog(AdminFrame.this, 
                            "Lỗi: " + errorMsg, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
                
                return null;
            }
        };
        worker.execute();
    }
    
    private void loadAvatarPreview(JLabel previewLabel, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            previewLabel.setIcon(null);
            previewLabel.setText("👤");
            return;
        }
        
        // Use ImageCache for loading with caching
        ImageIcon cachedIcon = ImageCache.getInstance().getImage(avatarUrl, 90, 90, new ImageCache.ImageLoadCallback() {
            @Override
            public void onImageLoaded(ImageIcon icon) {
                SwingUtilities.invokeLater(() -> {
                    if (icon != null) {
                        previewLabel.setIcon(icon);
                        previewLabel.setText("");
                    } else {
                        previewLabel.setIcon(null);
                        previewLabel.setText("❌");
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
            previewLabel.setText("🔄");
        }
    }
    
    private JPanel createActivityLogPanel() {
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBackground(new Color(246, 250, 252)); // #F6FAFC
        logPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        
        // Card container with shadow and radius
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
            BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));
    // Allow the card to expand vertically so it reaches closer to the bottom of the window
    cardPanel.setPreferredSize(new Dimension(1200, 900)); // initial preferred height
    cardPanel.setMaximumSize(new Dimension(1200, 10000));
        
    // Center the card and let it expand to fill vertical space
    JPanel centerWrapper = new JPanel(new BorderLayout());
    centerWrapper.setOpaque(false);
    centerWrapper.add(cardPanel, BorderLayout.CENTER);
    // Give wrapper a flexible minimum size so BorderLayout stretches the card
    centerWrapper.setPreferredSize(new Dimension(0, 0));
    logPanel.add(centerWrapper, BorderLayout.CENTER);
        
        // Header inside card - gradient style
        JPanel logHeader = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(45, 156, 219), // #2D9CDB
                    getWidth(), 0, new Color(59, 130, 246) // #3B82F6
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Subtle shadow at bottom
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fillRect(0, getHeight() - 2, getWidth(), 2);
                
                g2d.dispose();
            }
        };
        logHeader.setLayout(new BorderLayout());
        logHeader.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24)); // 20px padding
        logHeader.setPreferredSize(new Dimension(0, 80)); // height 80px
        
        JLabel logTitle = new JLabel("Lịch sử hoạt động");
        logTitle.setFont(new Font("Segoe UI", Font.BOLD, 24)); // 24px semibold
        logTitle.setForeground(Color.WHITE); // White text on gradient
        logHeader.add(logTitle, BorderLayout.WEST);
        
        JButton refreshLogBtn = new JButton("Làm mới");
        refreshLogBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshLogBtn.setBackground(new Color(255, 255, 255, 200)); // Semi-transparent white
        refreshLogBtn.setForeground(new Color(45, 156, 219)); // #2D9CDB
        refreshLogBtn.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        refreshLogBtn.setFocusPainted(false);
        refreshLogBtn.setPreferredSize(new Dimension(100, 40)); // height 40px
        refreshLogBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 1, true),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        // Hover effect
        refreshLogBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                refreshLogBtn.setBackground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                refreshLogBtn.setBackground(new Color(255, 255, 255, 200));
            }
        });
        refreshLogBtn.addActionListener(e -> loadActivityLogs());
        logHeader.add(refreshLogBtn, BorderLayout.EAST);
        
        cardPanel.add(logHeader, BorderLayout.NORTH);
        
        // Pagination controls
        JButton prevPageBtn = new JButton("‹ Trước");
        prevPageBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        prevPageBtn.setPreferredSize(new Dimension(90, 36));
        prevPageBtn.setFocusPainted(false);
        prevPageBtn.addActionListener(e -> {
            if (activityLogPage > 1) {
                activityLogPage--;
                loadActivityLogs();
            }
        });

        JLabel pageLabel = new JLabel("Trang " + activityLogPage);
        pageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton nextPageBtn = new JButton("Kế tiếp ›");
        nextPageBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        nextPageBtn.setPreferredSize(new Dimension(90, 36));
        nextPageBtn.setFocusPainted(false);
        nextPageBtn.addActionListener(e -> {
            activityLogPage++;
            loadActivityLogs();
        });
        
        // Log table
        String[] logColumns = {"Thời gian", "Người dùng", "Hành động", "Chi tiết"};
        DefaultTableModel logTableModel = new DefaultTableModel(logColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable logTable = new JTable(logTableModel);
        logTable.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // 14px #111827
        logTable.setRowHeight(48); // 48px
        logTable.setGridColor(new Color(241, 245, 249)); // #F1F5F9
        logTable.setShowVerticalLines(false);
        logTable.setShowHorizontalLines(true);
        logTable.setSelectionBackground(new Color(219, 234, 254));
        logTable.setIntercellSpacing(new Dimension(0, 0)); // Remove cell spacing
        
        // Header styling
        logTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14)); // 14px semibold
        logTable.getTableHeader().setBackground(Color.WHITE); // transparent
        logTable.getTableHeader().setForeground(new Color(55, 65, 81)); // #374151
        logTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 233, 239))); // #E6E9EF
        logTable.getTableHeader().setPreferredSize(new Dimension(0, 56)); // height 56px
        
        // Column widths
        logTable.getColumnModel().getColumn(0).setPreferredWidth(220); // Thời gian
        logTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Người dùng
        logTable.getColumnModel().getColumn(2).setPreferredWidth(280); // Hành động
        logTable.getColumnModel().getColumn(3).setPreferredWidth(500); // Chi tiết (remaining)
        
        // Custom renderer for zebra stripes and hover
        logTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(Color.WHITE);
                
                if (!isSelected) {
                    if (row % 2 == 1) {
                        c.setBackground(new Color(251, 253, 255)); // #FBFDFF
                    }
                } else {
                    c.setBackground(new Color(219, 234, 254)); // selection color
                }
                
                // Padding
                if (c instanceof JLabel) {
                    ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18)); // 12px 18px
                }
                
                // Font for timestamp column
                if (column == 0) {
                    c.setFont(new Font("Consolas", Font.PLAIN, 13)); // monospace for timestamp
                    c.setForeground(new Color(31, 41, 55)); // #1F2937
                } else {
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    c.setForeground(new Color(17, 24, 39)); // #111827
                }
                
                return c;
            }
        });
        
        // Add hover effect for rows
        final int[] hoveredRow = {-1};
        logTable.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = logTable.rowAtPoint(e.getPoint());
                if (row != hoveredRow[0]) {
                    hoveredRow[0] = row;
                    logTable.repaint();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow[0] = -1;
                logTable.repaint();
            }
        });
        
        // Custom renderer for zebra stripes and hover
        logTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(Color.WHITE);
                
                if (!isSelected) {
                    if (row % 2 == 1) {
                        c.setBackground(new Color(251, 253, 255)); // #FBFDFF
                    }
                    // Hover effect
                    if (row == hoveredRow[0]) {
                        c.setBackground(new Color(45, 156, 219, 10)); // rgba(45,156,219,0.04)
                    }
                } else {
                    c.setBackground(new Color(219, 234, 254)); // selection color
                }
                
                // Padding
                if (c instanceof JLabel) {
                    ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18)); // 12px 18px
                }
                
                // Font for timestamp column
                if (column == 0) {
                    c.setFont(new Font("Consolas", Font.PLAIN, 13)); // monospace for timestamp
                    c.setForeground(new Color(31, 41, 55)); // #1F2937
                } else {
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    c.setForeground(new Color(17, 24, 39)); // #111827
                }
                
                return c;
            }
        });
        
        JScrollPane logScrollPane = new JScrollPane(logTable);
        logScrollPane.setBorder(BorderFactory.createEmptyBorder());
        logScrollPane.getViewport().setBackground(Color.WHITE);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        logScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // Custom scrollbar styling
        JScrollBar verticalScrollBar = logScrollPane.getVerticalScrollBar();
        verticalScrollBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(230, 233, 239); // #E6E9EF
                this.trackColor = new Color(243, 244, 246); // #F3F4F6
            }
            
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            
            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
        verticalScrollBar.setPreferredSize(new Dimension(10, 0)); // width 10px
        
        cardPanel.add(logScrollPane, BorderLayout.CENTER);

        // Pagination footer (modern) - placed at bottom
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pager.setOpaque(false);

        // Style buttons to be modern: pill-like, subtle shadow
        prevPageBtn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        prevPageBtn.setBackground(new Color(249, 250, 251));
        prevPageBtn.setForeground(new Color(55, 65, 81));
        prevPageBtn.setFocusPainted(false);

        nextPageBtn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        nextPageBtn.setBackground(new Color(45, 156, 219));
        nextPageBtn.setForeground(Color.WHITE);
        nextPageBtn.setFocusPainted(false);

        pageLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        pageLabel.setOpaque(true);
        pageLabel.setBackground(new Color(250, 251, 252));

        pager.add(prevPageBtn);
        pager.add(pageLabel);
        pager.add(nextPageBtn);

        footer.add(pager, BorderLayout.CENTER);
        cardPanel.add(footer, BorderLayout.SOUTH);

        // Store reference to table model and pagination controls for loading data
        // NOTE: put the property on the outer `logPanel` so callers that search
        // mainContentPanel.getComponents() can find it (they iterate top-level panels)
        cardPanel.putClientProperty("tableModel", logTableModel);
        cardPanel.putClientProperty("table", logTable);
        cardPanel.putClientProperty("pageLabel", pageLabel);
        cardPanel.putClientProperty("prevPageBtn", prevPageBtn);
        cardPanel.putClientProperty("nextPageBtn", nextPageBtn);
        logPanel.putClientProperty("tableModel", logTableModel);
        logPanel.putClientProperty("table", logTable);
        logPanel.putClientProperty("pageLabel", pageLabel);
        logPanel.putClientProperty("prevPageBtn", prevPageBtn);
        logPanel.putClientProperty("nextPageBtn", nextPageBtn);
        
        return logPanel;
    }
    
    private JPanel createUserTablePanel() {
        // Modern card-style table container with elevation
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBackground(new Color(250, 251, 252)); // Very light background
        userPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        
        // Create main card container with shadow - giống trang cập nhật thông tin người dùng
        JPanel mainCardPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw shadow
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fillRoundRect(10, 10, getWidth() - 10, getHeight() - 10, 20, 20);
                
                // Draw card background
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth() - 10, getHeight() - 10, 20, 20);
                
                g2d.dispose();
            }
        };
        mainCardPanel.setOpaque(false);
        mainCardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Content panel
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Table toolbar
        JPanel toolbar = createUserTableToolbar();
        cardPanel.add(toolbar, BorderLayout.NORTH);
        
        // Existing table functionality
        JPanel tablePanel = createTablePanel();
        
        // Add table selection listener for enabling/disabling buttons
        if (userTable != null) {
            userTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    updateButtonStates();
                }
            });
        }
        
        cardPanel.add(tablePanel, BorderLayout.CENTER);

        // Pagination section positioned above the status bar
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setOpaque(false);
        bottomContainer.add(createPaginationSection(false), BorderLayout.NORTH);
        
        // Status panel with modern styling
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(249, 250, 251));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 231, 235)),
            BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        statusPanel.setPreferredSize(new Dimension(0, 44));
        
        statusLabel = new JLabel("Sẵn sàng");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(107, 114, 128));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(200, 20));
        statusPanel.add(progressBar, BorderLayout.EAST);

        bottomContainer.add(statusPanel, BorderLayout.SOUTH);
        cardPanel.add(bottomContainer, BorderLayout.SOUTH);
        mainCardPanel.add(cardPanel, BorderLayout.CENTER);
        userPanel.add(mainCardPanel, BorderLayout.CENTER);
        
        return userPanel;
    }
    
    private JPanel createUserTableToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 242, 247)),
            BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));
        
        // Left: Title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        
        JLabel tableTitle = new JLabel("Danh sách tài khoản");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableTitle.setForeground(new Color(51, 65, 85));
        leftPanel.add(tableTitle);
        
        toolbar.add(leftPanel, BorderLayout.WEST);
        
        // Right: Controls
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);
        
        // Search field with search icon
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setOpaque(false);
        searchPanel.setPreferredSize(new Dimension(280, 40));
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(0, 12, 0, 12)
        ));
        searchPanel.setBackground(Color.WHITE);
        
        JLabel searchIcon = new JLabel("");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchIcon.setForeground(new Color(156, 163, 175));
        searchPanel.add(searchIcon, BorderLayout.WEST);
        
        globalSearchField = new JTextField("Tìm theo tên, email, ID");
        globalSearchField.setBorder(null);
        globalSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        globalSearchField.setForeground(new Color(156, 163, 175));
        globalSearchField.setOpaque(false);
        globalSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterUsers(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterUsers(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterUsers(); }
        });
        
        // Add placeholder effect
        globalSearchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (globalSearchField.getText().equals("Tìm theo tên, email, ID")) {
                    globalSearchField.setText("");
                    globalSearchField.setForeground(new Color(55, 65, 81));
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (globalSearchField.getText().isEmpty()) {
                    globalSearchField.setText("Tìm theo tên, email, ID");
                    globalSearchField.setForeground(new Color(156, 163, 175));
                }
            }
        });
        
        searchPanel.add(globalSearchField, BorderLayout.CENTER);
        rightPanel.add(searchPanel);
        
        // Role filter
        roleFilter = new JComboBox<>(new String[]{"Tất cả vai trò", "NGƯỜI DÙNG", "QUẢN TRỊ VIÊN"});
        roleFilter.setPreferredSize(new Dimension(140, 40));
        roleFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        roleFilter.setBackground(Color.WHITE);
        roleFilter.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        // Thiết lập renderer để chữ "Tất cả vai trò" có màu đen
        roleFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setForeground(Color.BLACK); // Màu đen cho tất cả các item
                if (isSelected) {
                    setBackground(new Color(45, 156, 219));
                    setForeground(Color.WHITE);
                } else {
                    setBackground(Color.WHITE);
                }
                return this;
            }
        });
        roleFilter.addActionListener(e -> filterUsers());
        rightPanel.add(roleFilter);
        
        // Status filter
        statusFilter = new JComboBox<>(new String[]{"Tất cả trạng thái", "HOẠT ĐỘNG", "BỊ KHÓA"});
        statusFilter.setPreferredSize(new Dimension(140, 40));
        statusFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusFilter.setBackground(Color.WHITE);
        statusFilter.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        // Thiết lập renderer để chữ "Tất cả trạng thái" có màu đen
        statusFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setForeground(Color.BLACK); // Màu đen cho tất cả các item
                if (isSelected) {
                    setBackground(new Color(45, 156, 219));
                    setForeground(Color.WHITE);
                } else {
                    setBackground(Color.WHITE);
                }
                return this;
            }
        });
        statusFilter.addActionListener(e -> filterUsers());
        rightPanel.add(statusFilter);
        
        // Note: Export and Add Account buttons removed per UI requirement.
        // Keep placeholders for compat with existing code paths
        createUserBtn = new JButton("Thêm tài khoản");
        createUserBtn.setVisible(false);
        refreshBtn = new JButton("Làm mới");
        editUserBtn = new JButton("Sửa");
        editUserBtn.setEnabled(false);
        deleteUserBtn = new JButton("Xóa");
        deleteUserBtn.setEnabled(false);
        
        // instantiate other buttons for compatibility but don't show them in toolbar
        refreshBtn = new JButton("Làm mới");
        editUserBtn = new JButton("Sửa");
        editUserBtn.setEnabled(false);
        deleteUserBtn = new JButton("Xóa");
        deleteUserBtn.setEnabled(false);
        
        toolbar.add(rightPanel, BorderLayout.EAST);
        
        return toolbar;
    }
    
    private JPanel createUpdateUsersPanel() {
        // Modern card-style table container with elevation - matching main Users panel exactly
        JPanel updatePanel = new JPanel(new BorderLayout());
        // mark this panel so we can find it reliably when updating data
        updatePanel.putClientProperty("panelName", "UPDATE_USERS");
    updatePanel.setBackground(new Color(250, 251, 252)); // Match users list background
        updatePanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        
        // Create main card container with shadow
        JPanel mainCardPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw shadow
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fillRoundRect(10, 10, getWidth() - 10, getHeight() - 10, 20, 20);
                
                // Draw card background
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth() - 10, getHeight() - 10, 20, 20);
                
                g2d.dispose();
            }
        };
        mainCardPanel.setOpaque(false);
        mainCardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Không cần header panel phức tạp nữa
        
        // Content panel
    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBackground(Color.WHITE);
    contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Table toolbar - using same styling as main
        JPanel toolbar = createUpdateUsersToolbar();
        contentPanel.add(toolbar, BorderLayout.NORTH);
        
        // Table panel with modern styling
        JPanel tablePanel = createUpdateTablePanel();
        contentPanel.add(tablePanel, BorderLayout.CENTER);
        
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setOpaque(false);
        bottomContainer.add(createPaginationSection(true), BorderLayout.NORTH);

        // Status panel with modern styling - same as main
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(249, 250, 251));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 231, 235)),
            BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        statusPanel.setPreferredSize(new Dimension(0, 44));
        
        JLabel statusLabel = new JLabel("Sẵn sàng");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(107, 114, 128));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(200, 20));
        statusPanel.add(progressBar, BorderLayout.EAST);
        
        bottomContainer.add(statusPanel, BorderLayout.SOUTH);
        contentPanel.add(bottomContainer, BorderLayout.SOUTH);
        mainCardPanel.add(contentPanel, BorderLayout.CENTER);
        updatePanel.add(mainCardPanel, BorderLayout.CENTER);
        
        return updatePanel;
    }
    
    private JPanel createUpdateUsersToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 242, 247)),
            BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));
        
        // Left: Title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        
        JLabel tableTitle = new JLabel("Cập nhật thông tin người dùng");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableTitle.setForeground(new Color(51, 65, 85));
        leftPanel.add(tableTitle);
        
        toolbar.add(leftPanel, BorderLayout.WEST);
        
        // Right: Controls
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);
        
        // Search field with search icon
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setOpaque(false);
        searchPanel.setPreferredSize(new Dimension(280, 40));
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(0, 12, 0, 12)
        ));
        searchPanel.setBackground(Color.WHITE);
        
        JLabel searchIcon = new JLabel("");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchIcon.setForeground(new Color(156, 163, 175));
        searchPanel.add(searchIcon, BorderLayout.WEST);
        
        updateSearchField = new JTextField("Tìm theo tên, email, ID");
        updateSearchField.setBorder(null);
        updateSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        updateSearchField.setForeground(new Color(156, 163, 175));
        updateSearchField.setOpaque(false);
        updateSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterUpdateUsers(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterUpdateUsers(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterUpdateUsers(); }
        });
        
        // Add placeholder effect
        updateSearchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (updateSearchField.getText().equals("Tìm theo tên, email, ID")) {
                    updateSearchField.setText("");
                    updateSearchField.setForeground(new Color(55, 65, 81));
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (updateSearchField.getText().isEmpty()) {
                    updateSearchField.setText("Tìm theo tên, email, ID");
                    updateSearchField.setForeground(new Color(156, 163, 175));
                }
            }
        });
        
        searchPanel.add(updateSearchField, BorderLayout.CENTER);
        rightPanel.add(searchPanel);
        
        // Role filter
        updateRoleFilter = new JComboBox<>(new String[]{"Tất cả vai trò", "NGƯỜI DÙNG", "QUẢN TRỊ VIÊN"});
        updateRoleFilter.setPreferredSize(new Dimension(140, 40));
        updateRoleFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        updateRoleFilter.setBackground(Color.WHITE);
        updateRoleFilter.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        // Thiết lập renderer để chữ "Tất cả vai trò" có màu đen
        updateRoleFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setForeground(Color.BLACK); // Màu đen cho tất cả các item
                if (isSelected) {
                    setBackground(new Color(45, 156, 219));
                    setForeground(Color.WHITE);
                } else {
                    setBackground(Color.WHITE);
                }
                return this;
            }
        });
        updateRoleFilter.addActionListener(e -> filterUpdateUsers());
        rightPanel.add(updateRoleFilter);
        
        // Status filter
        updateStatusFilter = new JComboBox<>(new String[]{"Tất cả trạng thái", "HOẠT ĐỘNG", "BỊ KHÓA"});
        updateStatusFilter.setPreferredSize(new Dimension(140, 40));
        updateStatusFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        updateStatusFilter.setBackground(Color.WHITE);
        updateStatusFilter.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        // Thiết lập renderer để chữ "Tất cả trạng thái" có màu đen
        updateStatusFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setForeground(Color.BLACK); // Màu đen cho tất cả các item
                if (isSelected) {
                    setBackground(new Color(45, 156, 219));
                    setForeground(Color.WHITE);
                } else {
                    setBackground(Color.WHITE);
                }
                return this;
            }
        });
        updateStatusFilter.addActionListener(e -> filterUpdateUsers());
        rightPanel.add(updateStatusFilter);
        
        toolbar.add(rightPanel, BorderLayout.EAST);
        
        return toolbar;
    }
    
    private JPanel createUpdateTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        
        // Create table model with modern column setup - same as main
        String[] modernColumnNames = {
            "ID", "Avatar", "Tài khoản", "Email", "Vai trò", "Trạng thái", "Online", "Ngày tạo", "Thao tác"
        };
        
        DefaultTableModel updateTableModel = new DefaultTableModel(modernColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 8; // Only action column is editable
            }
        };
        
        JTable updateUserTable = new JTable(updateTableModel);
        
        // Modern table styling - exact copy from main
        updateUserTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        updateUserTable.setRowHeight(58); // Optimized row height for content display
        updateUserTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        updateUserTable.setShowVerticalLines(false);
        updateUserTable.setShowHorizontalLines(true);
        updateUserTable.setGridColor(new Color(240, 242, 247));
        updateUserTable.setSelectionBackground(new Color(219, 234, 254));
        updateUserTable.setSelectionForeground(new Color(30, 64, 175));
        updateUserTable.setBackground(Color.WHITE);
        updateUserTable.setIntercellSpacing(new Dimension(0, 1));
        
        // Modern header styling with accent gradient - same as main
        updateUserTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        updateUserTable.getTableHeader().setBackground(new Color(99, 102, 241)); // Purple accent
        updateUserTable.getTableHeader().setForeground(Color.WHITE);
        updateUserTable.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
        updateUserTable.getTableHeader().setPreferredSize(new Dimension(0, 48));
        
        // Custom cell renderer for modern look with avatars - same as main
        updateUserTable.setDefaultRenderer(Object.class, new ModernTableCellRenderer());
        
        // Set custom editor for action column - using update-specific renderer/editor
    updateUserTable.getColumnModel().getColumn(8).setCellRenderer(new UpdateActionCellRenderer());
    updateUserTable.getColumnModel().getColumn(8).setCellEditor(new UpdateActionCellEditor());

        TableColumnModel updateColumnModel = updateUserTable.getColumnModel();
        updateColumnModel.getColumn(0).setPreferredWidth(50);   // ID (smaller)
        updateColumnModel.getColumn(1).setPreferredWidth(50);   // Avatar (smaller)
        updateColumnModel.getColumn(2).setPreferredWidth(100);  // Account (reduced)
        updateColumnModel.getColumn(3).setPreferredWidth(200);  // Email (reduced)
        updateColumnModel.getColumn(4).setPreferredWidth(120);  // Role (smaller)
        updateColumnModel.getColumn(5).setPreferredWidth(120);  // Status (smaller)
        updateColumnModel.getColumn(6).setPreferredWidth(180);  // Online status (increased for full info)
        updateColumnModel.getColumn(7).setPreferredWidth(100);  // Created Date (reduced)
        updateColumnModel.getColumn(8).setPreferredWidth(180);  // Actions (optimized for 2 buttons)
        
        // Hover effect - subtle highlight - same as main
        updateUserTable.addMouseMotionListener(new MouseAdapter() {
            private int lastRow = -1;
            
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = updateUserTable.rowAtPoint(e.getPoint());
                if (row != lastRow) {
                    lastRow = row;
                    updateUserTable.repaint();
                }
            }
        });
        
        // Remove double-click edit to focus on action column only - same as main
        updateUserTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                updateUserTable.repaint(); // Clear hover effect
            }
        });
        
        // Modern scroll pane - same as main
        JScrollPane scrollPane = new JScrollPane(updateUserTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        
        // Store references for later use
        tablePanel.putClientProperty("table", updateUserTable);
        tablePanel.putClientProperty("tableModel", updateTableModel);
        
        return tablePanel;
    }
    
    private void loadUpdateUsersData() {
        // Load users data and update the update table
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Request request = new Request("ADMIN_LIST_USERS");
                request.put("requestedBy", currentUser.getUsername());
                
                Response response = networkClient.send(request);
                if (response != null && response.isSuccess()) {
                    String usersStr = response.getData().get("users");
                    if (usersStr != null && !usersStr.trim().isEmpty()) {
                        users.clear();
                        String[] userEntries = usersStr.split(";");
                        for (String entry : userEntries) {
                            if (!entry.trim().isEmpty()) {
                                String[] parts = entry.split(",", -1);
                                if (parts.length >= 10) {
                                    User user = new User();
                                    try {
                                        user.setId(Integer.parseInt(parts[0]));
                                        user.setUsername(parts[1]);
                                        user.setFullName(parts[2]);
                                        user.setEmail(parts[3]);
                                        user.setAvatar(parts[4]);
                                        user.setRole(parts[5]);
                                        user.setStatus(parts[6]);

                                        String onlineStatus = parts[7];
                                        String lastLogin = parts[8];
                                        String createdAt = parts[9];

                                        user.setLastLogin(lastLogin != null && !lastLogin.trim().isEmpty() ? lastLogin : "Chưa đăng nhập");
                                        user.setCreatedAt(createdAt != null ? createdAt : "");

                                        onlineUsers.put(user.getUsername(), "ONLINE".equalsIgnoreCase(onlineStatus));
                                        users.add(user);
                                    } catch (NumberFormatException e) {
                                        System.err.println("Error parsing user data: " + entry);
                                    }
                                }
                            }
                        }
                    }
                }
                return null;
            }
            
            @Override
            protected void done() {
                // Find and update the update table
                findAndUpdateUpdateTable();
            }
        };
        worker.execute();
    }
    
    private void findAndUpdateUpdateTable() {
        // Find the UPDATE_USERS panel by client property and update its table
        Component[] components = mainContentPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                Object name = panel.getClientProperty("panelName");
                if (name != null && "UPDATE_USERS".equals(name)) {
                    // Re-apply the update-panel filters so the view matches current controls
                    filterUpdateUsers();
                    break;
                }
            }
        }
    }
    
    private JTable findTableInContainer(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JTable) {
                return (JTable) comp;
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                if (scrollPane.getViewport().getView() instanceof JTable) {
                    return (JTable) scrollPane.getViewport().getView();
                }
            } else if (comp instanceof Container) {
                JTable found = findTableInContainer((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    private void updateUpdateTableData(DefaultTableModel model, List<User> users) {
        model.setRowCount(0);
        
        // Calculate pagination
        int totalFilteredUsers = users.size();
        totalUpdateUserPages = (int) Math.ceil((double) totalFilteredUsers / USERS_PER_PAGE);
        if (totalUpdateUserPages == 0) totalUpdateUserPages = 1;
        if (updateUsersPage > totalUpdateUserPages) updateUsersPage = totalUpdateUserPages;
        if (updateUsersPage < 1) updateUsersPage = 1;
        
        // Get users for current page
        int startIndex = (updateUsersPage - 1) * USERS_PER_PAGE;
        int endIndex = Math.min(startIndex + USERS_PER_PAGE, totalFilteredUsers);
        
        for (int i = startIndex; i < endIndex; i++) {
            User user = users.get(i);
            Object[] row = new Object[9];
            row[0] = user.getId();

            row[1] = user.getAvatar() != null ? user.getAvatar() : "";

            String displayName = user.getFullName() != null ? user.getFullName().trim() : "";
            row[2] = new UserDisplayInfo(
                user.getUsername() != null ? user.getUsername() : "",
                displayName
            );

            row[3] = user.getEmail() != null ? user.getEmail() : "";

            row[4] = new StatusChip(
                UIUtils.mapRoleToVietnamese(user.getRole()),
                "ADMIN".equalsIgnoreCase(user.getRole()) ? StatusChip.Type.INFO : StatusChip.Type.SECONDARY
            );

            String vietnameseStatus = UIUtils.mapStatusToVietnamese(user.getStatus());
            StatusChip.Type statusType = "HOẠT ĐỘNG".equalsIgnoreCase(vietnameseStatus)
                ? StatusChip.Type.SUCCESS
                : StatusChip.Type.DANGER;
            row[5] = new StatusChip(vietnameseStatus, statusType);

            boolean isOnline = onlineUsers.getOrDefault(user.getUsername(), false);
            row[6] = new OnlineStatus(
                isOnline,
                isOnline ? "TRỰC TUYẾN" : "NGOẠI TUYẾN",
                formatLastLoginTime(user.getLastLogin())
            );

            // Use the raw createdAt value from the database (it's already formatted there)
            row[7] = user.getCreatedAt() != null ? user.getCreatedAt() : "N/A";

            row[8] = user;

            model.addRow(row);
        }
        
        updateUpdatePaginationControls();
    }
    
    private void updateUpdatePaginationControls() {
        // Update update users pagination controls
        Component[] components = getContentPane().getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                updatePaginationInPanel((JPanel) comp, updateUsersPage, totalUpdateUserPages, "updateUsers");
            }
        }
    }
    private JPanel createPaginationSection(boolean forUpdatePanel) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(new Color(249, 250, 251));
        container.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 231, 235)),
            BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        controls.setOpaque(false);
        controls.putClientProperty("paginationType", forUpdatePanel ? "updateUsers" : "userTable");

        JButton prevBtn = new JButton("‹ Trước");
        prevBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        prevBtn.setPreferredSize(new Dimension(80, 36));
        prevBtn.setFocusPainted(false);
        prevBtn.setEnabled(forUpdatePanel ? updateUsersPage > 1 : userTablePage > 1);
        prevBtn.addActionListener(e -> {
            if (forUpdatePanel) {
                if (updateUsersPage > 1) {
                    updateUsersPage--;
                    loadUpdateUsersData();
                }
            } else {
                if (userTablePage > 1) {
                    userTablePage--;
                    loadUsers();
                }
            }
        });
        controls.add(prevBtn);

        int currentPage = forUpdatePanel ? updateUsersPage : userTablePage;
        int totalPages = forUpdatePanel ? totalUpdateUserPages : totalUserPages;
        JLabel pageLabel = new JLabel("Trang " + currentPage + " / " + totalPages);
        pageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        controls.add(pageLabel);

        JButton nextBtn = new JButton("Tiếp ›");
        nextBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        nextBtn.setPreferredSize(new Dimension(80, 36));
        nextBtn.setFocusPainted(false);
        nextBtn.setEnabled(forUpdatePanel ? updateUsersPage < totalUpdateUserPages : userTablePage < totalUserPages);
        nextBtn.addActionListener(e -> {
            if (forUpdatePanel) {
                if (updateUsersPage < totalUpdateUserPages) {
                    updateUsersPage++;
                    loadUpdateUsersData();
                }
            } else {
                if (userTablePage < totalUserPages) {
                    userTablePage++;
                    loadUsers();
                }
            }
        });
        controls.add(nextBtn);

        container.add(controls, BorderLayout.CENTER);
        return container;
    }

    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        
        // Create table model with modern column setup
        String[] modernColumnNames = {
            "ID", "Avatar", "Tài khoản", "Email", "Vai trò", "Trạng thái", "Online", "Ngày tạo", "Thao tác"
        };
        
        tableModel = new DefaultTableModel(modernColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 8; // Only action column is editable
            }
        };
        
        userTable = new JTable(tableModel);
        
        // Modern table styling
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userTable.setRowHeight(58); // Optimized row height for content display
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setShowVerticalLines(false);
        userTable.setShowHorizontalLines(true);
        userTable.setGridColor(new Color(240, 242, 247));
        userTable.setSelectionBackground(new Color(219, 234, 254));
        userTable.setSelectionForeground(new Color(30, 64, 175));
        userTable.setBackground(Color.WHITE);
        userTable.setIntercellSpacing(new Dimension(0, 1));
        
        // Modern header styling with accent gradient
        userTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        userTable.getTableHeader().setBackground(new Color(99, 102, 241)); // Purple accent
        userTable.getTableHeader().setForeground(Color.WHITE);
        userTable.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
        userTable.getTableHeader().setPreferredSize(new Dimension(0, 48));
        
        // Custom cell renderer for modern look with avatars
        userTable.setDefaultRenderer(Object.class, new ModernTableCellRenderer());
        
        // Set custom editor for action column
        userTable.getColumnModel().getColumn(8).setCellRenderer(new ActionCellRenderer());
        userTable.getColumnModel().getColumn(8).setCellEditor(new ActionCellEditor());
        
    // Modern column widths - optimized for better display
    TableColumnModel columnModel = userTable.getColumnModel();
    columnModel.getColumn(0).setPreferredWidth(50);   // ID (smaller)
    columnModel.getColumn(1).setPreferredWidth(50);   // Avatar (smaller)
    columnModel.getColumn(2).setPreferredWidth(100);  // Account (reduced)
    columnModel.getColumn(3).setPreferredWidth(200);  // Email (reduced)
    columnModel.getColumn(4).setPreferredWidth(120);  // Role (smaller)
    columnModel.getColumn(5).setPreferredWidth(120);  // Status (smaller)
    columnModel.getColumn(6).setPreferredWidth(180);  // Online status (increased for full info)
    columnModel.getColumn(7).setPreferredWidth(100);  // Created Date (reduced)
    columnModel.getColumn(8).setPreferredWidth(180);  // Actions (optimized for 2 buttons)
        
        // Hover effect - subtle highlight
        userTable.addMouseMotionListener(new MouseAdapter() {
            private int lastRow = -1;
            
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = userTable.rowAtPoint(e.getPoint());
                if (row != lastRow) {
                    lastRow = row;
                    userTable.repaint();
                }
            }
        });
        
        // Remove double-click edit to focus on action column only
        userTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                userTable.repaint(); // Clear hover effect
            }
        });
        
        // Modern scroll pane
        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        
        return tablePanel;
    }
    
    private void updateButtonStates() {
        int selectedRow = userTable.getSelectedRow();
        boolean hasSelection = selectedRow >= 0;
        
        editUserBtn.setEnabled(hasSelection);
        deleteUserBtn.setEnabled(hasSelection);
    }
    
    private void loadUsers() {
        SwingWorker<List<User>, Void> worker = new SwingWorker<List<User>, Void>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                setStatusAndProgress("Đang tải danh sách người dùng...", true);
                
                Request request = new Request("ADMIN_LIST_USERS");
                request.put("requestedBy", currentUser.getUsername());
                
                Response response = networkClient.send(request);
                if (response != null && response.isSuccess()) {
                    List<User> userList = new ArrayList<>();
                    String usersData = response.getData().get("users");
                    
                    if (usersData != null && !usersData.trim().isEmpty()) {
                        String[] userRecords = usersData.split(";");
                        for (String record : userRecords) {
                            if (!record.trim().isEmpty()) {
                                String[] parts = record.split(",", -1); // Keep empty strings
                                if (parts.length >= 10) { // Minimum required fields
                                    User user = new User();
                                    user.setId(Integer.parseInt(parts[0]));
                                    user.setUsername(parts[1]);
                                    user.setFullName(parts[2]);
                                    user.setEmail(parts[3]);
                                    user.setAvatar(parts[4]);
                                    user.setRole(parts[5]);
                                    user.setStatus(parts[6]);

                                    String onlineStatus = parts[7];
                                    String lastLogin = parts[8];
                                    String createdAt = parts[9];

                                    user.setLastLogin(lastLogin != null && !lastLogin.trim().isEmpty() ? lastLogin : "Chưa đăng nhập");
                                    user.setCreatedAt(createdAt != null ? createdAt : "");

                                    onlineUsers.put(user.getUsername(), "ONLINE".equalsIgnoreCase(onlineStatus));
                                    userList.add(user);
                                }
                            }
                        }
                    }
                    return userList;
                }
                return new ArrayList<>();
            }
            
            @Override
            protected void done() {
                try {
                    users = get();
                    // Cập nhật trạng thái online ngay sau khi load users
                    updateOnlineStatus();
                    // Cập nhật dashboard KPIs
                    updateDashboardKPIs();
                    setStatusAndProgress("Đã tải " + users.size() + " người dùng", false);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(AdminFrame.this, 
                        "Lỗi khi tải danh sách người dùng: " + e.getMessage(), 
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    setStatusAndProgress("Lỗi khi tải dữ liệu", false);
                }
            }
        };
        worker.execute();
    }
    
    private void filterUsers() {
        String searchText = globalSearchField.getText().toLowerCase().trim();
        // Handle placeholder text
        if ("tìm theo tên, email, id".equals(searchText)) {
            searchText = "";
        }
        
        String statusFilter = (String) this.statusFilter.getSelectedItem();
        String roleFilter = (String) this.roleFilter.getSelectedItem();
        
        filteredUsers.clear();
        
        for (User user : users) {
            String fullName = user.getFullName() != null ? user.getFullName() : "";
            String email = user.getEmail() != null ? user.getEmail() : "";
            String username = user.getUsername() != null ? user.getUsername() : "";
            
            boolean matchSearch = searchText.isEmpty() || 
                username.toLowerCase().contains(searchText) ||
                fullName.toLowerCase().contains(searchText) ||
                email.toLowerCase().contains(searchText) ||
                String.valueOf(user.getId()).contains(searchText);
            
            boolean matchStatus = "Tất cả trạng thái".equals(statusFilter) || 
                UIUtils.mapStatusToVietnamese(user.getStatus()).equals(statusFilter);
            
            boolean matchRole = "Tất cả vai trò".equals(roleFilter) || 
                UIUtils.mapRoleToVietnamese(user.getRole()).equals(roleFilter);
            
            if (matchSearch && matchStatus && matchRole) {
                filteredUsers.add(user);
            }
        }
        
        updateTableData();
    }

    // Filtering logic for the Update Users panel (separate controls)
    private void filterUpdateUsers() {
        if (updateSearchField == null || updateRoleFilter == null || updateStatusFilter == null) return;

        String searchText = updateSearchField.getText() != null ? updateSearchField.getText().toLowerCase().trim() : "";
        if ("tìm theo tên, email, id".equals(searchText)) searchText = "";

        String role = (String) updateRoleFilter.getSelectedItem();
        String status = (String) updateStatusFilter.getSelectedItem();

        List<User> results = new ArrayList<>();
        for (User user : users) {
            String fullName = user.getFullName() != null ? user.getFullName() : "";
            String email = user.getEmail() != null ? user.getEmail() : "";
            String username = user.getUsername() != null ? user.getUsername() : "";

            boolean matchSearch = searchText.isEmpty() || username.toLowerCase().contains(searchText)
                || fullName.toLowerCase().contains(searchText)
                || email.toLowerCase().contains(searchText)
                || String.valueOf(user.getId()).contains(searchText);

            boolean matchRole = "Tất cả vai trò".equals(role) || UIUtils.mapRoleToVietnamese(user.getRole()).equals(role);
            boolean matchStatus = "Tất cả trạng thái".equals(status) || UIUtils.mapStatusToVietnamese(user.getStatus()).equals(status);

            if (matchSearch && matchRole && matchStatus) results.add(user);
        }

        // Find update table and update its model
        Component[] components = mainContentPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                Object name = panel.getClientProperty("panelName");
                if (name != null && "UPDATE_USERS".equals(name)) {
                    JTable updateTable = findTableInContainer(panel);
                    if (updateTable != null && updateTable.getModel() instanceof DefaultTableModel) {
                        DefaultTableModel model = (DefaultTableModel) updateTable.getModel();
                        updateUpdateTableData(model, results);
                    }
                    break;
                }
            }
        }
    }
    
    private void updateTableData() {
        tableModel.setRowCount(0);
        
        // Calculate pagination
        int totalFilteredUsers = filteredUsers.size();
        totalUserPages = (int) Math.ceil((double) totalFilteredUsers / USERS_PER_PAGE);
        if (totalUserPages == 0) totalUserPages = 1;
        if (userTablePage > totalUserPages) userTablePage = totalUserPages;
        if (userTablePage < 1) userTablePage = 1;
        
        // Get users for current page
        int startIndex = (userTablePage - 1) * USERS_PER_PAGE;
        int endIndex = Math.min(startIndex + USERS_PER_PAGE, totalFilteredUsers);
        
        for (int i = startIndex; i < endIndex; i++) {
            User user = filteredUsers.get(i);
            Object[] row = new Object[9];
            row[0] = user.getId();

            // Avatar URL for dedicated avatar column
            row[1] = user.getAvatar() != null ? user.getAvatar() : "";

            // Account info: username (top) + @fullname (bottom)
            String displayName = user.getFullName() != null ? user.getFullName().trim() : "";
            row[2] = new UserDisplayInfo(
                user.getUsername() != null ? user.getUsername() : "",
                displayName
            );

            // Email column
            row[3] = user.getEmail() != null ? user.getEmail() : "";

            // Role with chip styling
            row[4] = new StatusChip(
                UIUtils.mapRoleToVietnamese(user.getRole()),
                "ADMIN".equalsIgnoreCase(user.getRole()) ? StatusChip.Type.INFO : StatusChip.Type.SECONDARY
            );

            // Status with chip styling
            String vietnameseStatus = UIUtils.mapStatusToVietnamese(user.getStatus());
            StatusChip.Type statusType = "HOẠT ĐỘNG".equalsIgnoreCase(vietnameseStatus)
                ? StatusChip.Type.SUCCESS
                : StatusChip.Type.DANGER;
            row[5] = new StatusChip(vietnameseStatus, statusType);

            // Online status with dot indicator
            boolean isOnline = onlineUsers.getOrDefault(user.getUsername(), false);
            OnlineStatus onlineStatus = new OnlineStatus(
                isOnline,
        isOnline ? "TRỰC TUYẾN" : "NGOẠI TUYẾN",
                formatLastLoginTime(user.getLastLogin())
            );
            row[6] = onlineStatus;

            // Formatted created date
            // Use DB value directly
            row[7] = user.getCreatedAt() != null ? user.getCreatedAt() : "N/A";

            // Action - will be rendered with buttons
            row[8] = user; // Pass user object for actions

            tableModel.addRow(row);
        }
        
        updateButtonStates();
        updatePaginationControls();
    }
    
    private void updatePaginationControls() {
        // Update user table pagination controls
        Component[] components = getContentPane().getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                updatePaginationInPanel((JPanel) comp, userTablePage, totalUserPages, "userTable");
            }
        }
    }
    
    private void updatePaginationInPanel(JPanel panel, int currentPage, int totalPages, String tableType) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel childPanel = (JPanel) comp;

                JButton prevBtn = null;
                JButton nextBtn = null;
                JLabel pageLabel = null;

                for (Component innerComp : childPanel.getComponents()) {
                    if (innerComp instanceof JButton) {
                        JButton btn = (JButton) innerComp;
                        if ("‹ Trước".equals(btn.getText())) {
                            prevBtn = btn;
                        } else if ("Tiếp ›".equals(btn.getText())) {
                            nextBtn = btn;
                        }
                    } else if (innerComp instanceof JLabel) {
                        pageLabel = (JLabel) innerComp;
                    }
                }

                if (prevBtn != null && nextBtn != null && pageLabel != null) {
                    prevBtn.setEnabled(currentPage > 1);
                    nextBtn.setEnabled(currentPage < totalPages);
                    pageLabel.setText("Trang " + currentPage + " / " + totalPages);
                }

                updatePaginationInPanel(childPanel, currentPage, totalPages, tableType);
            }
        }
    }

    private void createUser() {
        UserEditDialog dialog = new UserEditDialog(this, "Tạo người dùng mới", null, networkClient);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            loadUsers();
        }
    }
    
    private void editUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) return;
        
        User selectedUser = filteredUsers.get(selectedRow);
        UserEditDialog dialog = new UserEditDialog(this, "Sửa thông tin người dùng", selectedUser, networkClient);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            loadUsers();
        }
    }
    
    private void deleteUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) return;
        
        User selectedUser = filteredUsers.get(selectedRow);
        
        int option = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn khóa người dùng '" + selectedUser.getUsername() + "'?\n(Tính năng xóa hoàn toàn chưa được triển khai)",
            "Xác nhận khóa người dùng", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (option == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    setStatusAndProgress("Đang khóa người dùng...", true);
                    
                    // DELETE_USER action not implemented on server yet
                    // For now, we'll disable the user instead
                    Request request = new Request("ADMIN_SET_STATUS");
                    request.put("id", String.valueOf(selectedUser.getId()));
                    request.put("status", "LOCKED");
                    
                    Response response = networkClient.send(request);
                    return response != null && response.isSuccess();
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            JOptionPane.showMessageDialog(AdminFrame.this, 
                                "Đã khóa người dùng thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                            loadUsers();
                        } else {
                            JOptionPane.showMessageDialog(AdminFrame.this, 
                                "Không thể khóa người dùng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                        setStatusAndProgress("Sẵn sàng", false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(AdminFrame.this, 
                            "Lỗi khi khóa người dùng: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                        setStatusAndProgress("Lỗi", false);
                    }
                }
            };
            worker.execute();
        }
    }
    

    

    
    private void setStatusAndProgress(String status, boolean showProgress) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            progressBar.setVisible(showProgress);
            if (showProgress) {
                progressBar.setIndeterminate(true);
            } else {
                progressBar.setIndeterminate(false);
            }
        });
    }
    
    private void startHeartbeat() {
        heartbeatTimer = new javax.swing.Timer(30000, e -> {
            updateOnlineStatus();
            updateDashboardKPIs();
        });
        heartbeatTimer.start();
        
        // Initial update
        updateOnlineStatus();
        updateDashboardKPIs();
    }
    
    private void updateDashboardKPIs() {
        SwingUtilities.invokeLater(() -> {
            // Update KPI values based on current data
            if (totalUsersLabel != null) {
                totalUsersLabel.setText(String.valueOf(users.size()));
            }
            
            if (onlineUsersLabel != null) {
                long onlineCount = onlineUsers.values().stream().mapToLong(b -> b ? 1 : 0).sum();
                onlineUsersLabel.setText(String.valueOf(onlineCount));
            }
            
            if (lockedUsersLabel != null) {
                long lockedCount = users.stream()
                    .mapToLong(u -> "LOCKED".equals(u.getStatus()) ? 1 : 0)
                    .sum();
                lockedUsersLabel.setText(String.valueOf(lockedCount));
            }
            
            if (newUsersLabel != null) {
                // Count users created in last 7 days
                long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
                long newUserCount = users.stream()
                    .mapToLong(u -> {
                        try {
                            if (u.getCreatedAt() != null && !u.getCreatedAt().isEmpty()) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                java.util.Date createdDate = sdf.parse(u.getCreatedAt());
                                return createdDate.getTime() > sevenDaysAgo ? 1 : 0;
                            }
                        } catch (Exception e) {
                            // Ignore parsing errors
                        }
                        return 0;
                    })
                    .sum();
                newUsersLabel.setText(String.valueOf(newUserCount));
            }
        });
    }
    
    private void updateOnlineStatus() {
        SwingWorker<Map<String, Boolean>, Void> worker = new SwingWorker<Map<String, Boolean>, Void>() {
            @Override
            protected Map<String, Boolean> doInBackground() throws Exception {
                Request request = new Request("GET_ONLINE_USERS");
                Response response = networkClient.send(request);
                
                if (response != null && response.isSuccess()) {
                    Map<String, Boolean> onlineMap = new ConcurrentHashMap<>();
                    String onlineUserIds = response.getData().get("onlineUserIds");
                    
                    if (onlineUserIds != null && !onlineUserIds.trim().isEmpty()) {
                        String[] userIds = onlineUserIds.split(",");
                        java.util.Set<Integer> onlineIdSet = new java.util.HashSet<>();
                        
                        for (String idStr : userIds) {
                            try {
                                onlineIdSet.add(Integer.parseInt(idStr.trim()));
                            } catch (NumberFormatException e) {
                                // Skip invalid IDs
                            }
                        }
                        
                        // Update online status for all users
                        for (User user : users) {
                            boolean isOnline = onlineIdSet.contains(user.getId());
                            onlineMap.put(user.getUsername(), isOnline);
                        }
                    }
                    
                    return onlineMap;
                }
                return new ConcurrentHashMap<>();
            }
            
            @Override
            protected void done() {
                try {
                    Map<String, Boolean> newOnlineUsers = get();
                    if (newOnlineUsers != null && !newOnlineUsers.isEmpty()) {
                        onlineUsers = newOnlineUsers;
                        // Gọi filterUsers để refresh toàn bộ bảng với trạng thái mới
                        filterUsers();
                    }
                } catch (Exception e) {
                    // Ignore heartbeat errors
                    System.err.println("Heartbeat error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private String formatLastLoginTime(String lastLogin) {
        if (lastLogin == null || lastLogin.isEmpty() || "Chưa đăng nhập".equals(lastLogin)) {
            return "Chưa có";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date d = sdf.parse(lastLogin);
            long diffMs = System.currentTimeMillis() - d.getTime();
            long minutes = diffMs / (60_000);
            long hours = minutes / 60;
            long days = hours / 24;
            if (minutes < 1) return "Vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            if (hours < 24) return hours + " giờ trước";
            if (days < 7) return days + " ngày trước";
            SimpleDateFormat display = new SimpleDateFormat("dd/MM/yyyy");
            return display.format(d);
        } catch (Exception e) {
            return lastLogin;
        }
    }
    
    private void showUserHistory(User user) {
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                Request request = new Request("GET_USER_HISTORY");
                request.put("username", user.getUsername());

                Response response = networkClient.send(request);
                if (response != null && response.isSuccess()) {
                    return response.getData().get("history");
                }
                return "";
            }

            @Override
            protected void done() {
                try {
                    String historyData = get();

                    // Tạo dialog hiện đại
                    JDialog historyDialog = new JDialog(AdminFrame.this, "Lịch sử truy cập - " + user.getUsername(), true);
                    historyDialog.setSize(1000, 700);
                    historyDialog.setLocationRelativeTo(AdminFrame.this);
                    historyDialog.setLayout(new BorderLayout());
                    historyDialog.getContentPane().setBackground(new Color(245, 247, 250));

                    // Header Panel với thông tin người dùng
                    JPanel headerPanel = new JPanel(new BorderLayout());
                    headerPanel.setBackground(Color.WHITE);
                    headerPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 225, 230)),
                        BorderFactory.createEmptyBorder(20, 24, 20, 24)
                    ));

                    // Avatar và thông tin người dùng
                    JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
                    userInfoPanel.setOpaque(false);

                    // Avatar
                    CircularAvatarLabel userAvatar = new CircularAvatarLabel();
                    userAvatar.setPreferredSize(new Dimension(48, 48));
                    userAvatar.setHorizontalAlignment(SwingConstants.CENTER);
                    if (user.getAvatar() != null && !user.getAvatar().trim().isEmpty()) {
                        ImageIcon avatarIcon = ImageCache.getInstance().getImage(user.getAvatar(), 48, 48, null);
                        if (avatarIcon != null) {
                            userAvatar.setIcon(createHighQualityCircularIcon(avatarIcon, 48));
                        } else {
                            userAvatar.setText("👤");
                            userAvatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                        }
                    } else {
                        userAvatar.setText("👤");
                        userAvatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                    }
                    userAvatar.setBackground(new Color(243, 244, 246));
                    userInfoPanel.add(userAvatar);

                    // Thông tin người dùng
                    JPanel userDetailsPanel = new JPanel(new GridLayout(2, 1, 0, 2));
                    userDetailsPanel.setOpaque(false);

                    JLabel userNameLabel = new JLabel(user.getFullName());
                    userNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    userNameLabel.setForeground(new Color(32, 41, 56));

                    JLabel userEmailLabel = new JLabel(user.getEmail() + " • " + user.getRole());
                    userEmailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    userEmailLabel.setForeground(new Color(120, 130, 140));

                    userDetailsPanel.add(userNameLabel);
                    userDetailsPanel.add(userEmailLabel);
                    userInfoPanel.add(userDetailsPanel);

                    headerPanel.add(userInfoPanel, BorderLayout.WEST);

                    // Nút điều khiển
                    JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                    controlPanel.setOpaque(false);

                    JButton refreshBtn = new JButton("Làm mới");
                    refreshBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    refreshBtn.setBackground(new Color(33, 123, 244));
                    refreshBtn.setForeground(Color.WHITE);
                    refreshBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
                    refreshBtn.setFocusPainted(false);
                    refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    refreshBtn.addActionListener(e -> {
                        // Refresh logic - reload history
                        showUserHistory(user);
                        historyDialog.dispose();
                    });

                    JButton closeBtn = new JButton("Đóng");
                    closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    closeBtn.setBackground(new Color(108, 117, 125));
                    closeBtn.setForeground(Color.WHITE);
                    closeBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
                    closeBtn.setFocusPainted(false);
                    closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    closeBtn.addActionListener(e -> historyDialog.dispose());

                    controlPanel.add(refreshBtn);
                    controlPanel.add(closeBtn);
                    headerPanel.add(controlPanel, BorderLayout.EAST);

                    historyDialog.add(headerPanel, BorderLayout.NORTH);

                    // Main content panel
                    JPanel contentPanel = new JPanel(new BorderLayout());
                    contentPanel.setBackground(new Color(245, 247, 250));
                    contentPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

                    // Stats panel
                    JPanel statsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
                    statsPanel.setOpaque(false);
                    statsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

                    // Calculate stats from history data (Audit entries, all actions)
                    int totalActivities = 0;
                    int successfulActions = 0;
                    int failedActions = 0;
                    Set<String> uniqueActionTypes = new HashSet<>();

                    if (historyData != null && !historyData.trim().isEmpty()) {
                        String[] historyLines = historyData.split("\n");
                        for (String rawLine : historyLines) {
                            if (rawLine == null) continue;
                            String line = rawLine.trim();
                            if (line.isEmpty()) continue;

                            String[] parts = line.split("\\|", -1);
                            if (parts.length < 5) {
                                continue;
                            }

                            String type = parts[0];
                            if (!"AUDIT".equalsIgnoreCase(type)) {
                                continue;
                            }

                            totalActivities++;

                            String actionRaw = parts[2] != null ? parts[2].trim() : "";
                            if (!actionRaw.isEmpty()) {
                                uniqueActionTypes.add(actionRaw.toUpperCase(Locale.ROOT));
                            }

                            String resultPart = parts[4] != null ? parts[4].trim().toLowerCase(Locale.ROOT) : "";
                            boolean isSuccess = resultPart.isEmpty() || resultPart.contains("thành công") || resultPart.contains("success") || resultPart.contains("ok") || resultPart.contains("completed");
                            if (isSuccess) {
                                successfulActions++;
                            } else {
                                failedActions++;
                            }
                        }
                    }

                    statsPanel.add(createStatCard("Tổng hoạt động", String.valueOf(totalActivities), new Color(33, 123, 244)));
                    statsPanel.add(createStatCard("Số loại hoạt động", String.valueOf(uniqueActionTypes.size()), new Color(94, 114, 228)));
                    statsPanel.add(createStatCard("Hoạt động thành công", String.valueOf(successfulActions), new Color(52, 152, 219)));
                    statsPanel.add(createStatCard("Hoạt động thất bại", String.valueOf(failedActions), new Color(231, 76, 60)));

                    contentPanel.add(statsPanel, BorderLayout.NORTH);

                    // History table panel
                    JPanel tablePanel = new JPanel(new BorderLayout());
                    tablePanel.setBackground(Color.WHITE);
                    tablePanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                        BorderFactory.createEmptyBorder(20, 20, 20, 20)
                    ));

                    // Table title
                    JLabel tableTitle = new JLabel("Chi tiết hoạt động");
                    tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    tableTitle.setForeground(new Color(32, 41, 56));
                    tableTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
                    tablePanel.add(tableTitle, BorderLayout.NORTH);

                    // Tạo bảng hiển thị lịch sử
                    String[] historyColumns = {"Thời gian", "Hoạt động", "Chi tiết", "IP", "Trạng thái"};
                    DefaultTableModel historyModel = new DefaultTableModel(historyColumns, 0) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                            return false;
                        }
                    };

                    JTable historyTable = new JTable(historyModel);
                    historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    historyTable.setRowHeight(45);
                    historyTable.setGridColor(new Color(241, 245, 249));
                    historyTable.setShowVerticalLines(false);
                    historyTable.setShowHorizontalLines(true);
                    historyTable.setSelectionBackground(new Color(219, 234, 254));
                    historyTable.setIntercellSpacing(new Dimension(0, 0));

                    // Header styling
                    historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
                    historyTable.getTableHeader().setBackground(Color.WHITE);
                    historyTable.getTableHeader().setForeground(new Color(55, 65, 81));
                    historyTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 233, 239)));
                    historyTable.getTableHeader().setPreferredSize(new Dimension(0, 48));

                    // Column widths
                    historyTable.getColumnModel().getColumn(0).setPreferredWidth(180); // Thời gian
                    historyTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Hoạt động
                    historyTable.getColumnModel().getColumn(2).setPreferredWidth(250); // Chi tiết
                    historyTable.getColumnModel().getColumn(3).setPreferredWidth(120); // IP
                    historyTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Trạng thái

                    // Custom renderer for status column
                    historyTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value,
                                boolean isSelected, boolean hasFocus, int row, int column) {
                            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                            label.setHorizontalAlignment(SwingConstants.CENTER);

                            String status = value != null ? value.toString() : "";
                            if ("Thành công".equals(status) || status.toLowerCase().contains("thành công")) {
                                label.setText("Thành công");
                                label.setForeground(new Color(46, 204, 113));
                                label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                            } else if ("Thất bại".equals(status) || status.toLowerCase().contains("thất bại")) {
                                label.setText("Thất bại");
                                label.setForeground(new Color(231, 76, 60));
                                label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                            } else {
                                label.setText(status);
                                label.setForeground(new Color(120, 130, 140));
                                label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                            }
                            return label;
                        }
                    });

                    // Parse dữ liệu lịch sử (Audit only)
                    if (historyData != null && !historyData.trim().isEmpty()) {
                        String[] historyLines = historyData.split("\n");
                        for (String rawLine : historyLines) {
                            if (rawLine == null) continue;
                            String line = rawLine.trim();
                            if (line.isEmpty()) continue;

                            String[] parts = line.split("\\|", -1);
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
                            String resultRaw = parts[4] != null ? parts[4].trim() : "";

                            String activityLabel = activityRaw;
                            String normalizedActivity = activityRaw != null ? activityRaw.replace(" ", "_").toUpperCase(Locale.ROOT) : "";
                            if (normalizedActivity.contains("LOGIN_SUCCESS")) {
                                activityLabel = "Đăng nhập thành công";
                            } else if (normalizedActivity.contains("LOGIN_FAILED") || normalizedActivity.contains("LOGIN_FAILURE")) {
                                activityLabel = "Đăng nhập thất bại";
                            } else if (normalizedActivity.contains("LOGOUT")) {
                                activityLabel = "Đăng xuất";
                            } else if (normalizedActivity.contains("PROFILE_UPDATE") || normalizedActivity.contains("EDIT_USER") || normalizedActivity.contains("EDIT")) {
                                activityLabel = "Cập nhật thông tin";
                            } else if (normalizedActivity.contains("PASSWORD_CHANGE")) {
                                activityLabel = "Đổi mật khẩu";
                            }

                            if (!activityLabel.equals(activityRaw) && activityRaw != null && !activityRaw.isEmpty()) {
                                activityLabel = activityLabel + " (" + activityRaw + ")";
                            }

                            String status;
                            String normalizedResult = resultRaw.toLowerCase(Locale.ROOT);
                            if (normalizedResult.isEmpty()) {
                                status = "Thành công";
                            } else if (normalizedResult.contains("thành công") || normalizedResult.contains("success") || normalizedResult.contains("completed") || normalizedResult.contains("ok")) {
                                status = "Thành công";
                            } else if (normalizedResult.contains("thất bại") || normalizedResult.contains("failed") || normalizedResult.contains("error") || normalizedResult.contains("denied")) {
                                status = "Thất bại";
                            } else {
                                status = resultRaw;
                            }

                            String ip = "N/A";
                            Matcher matcher = IP_PATTERN.matcher(details);
                            if (matcher.find()) {
                                ip = matcher.group();
                            }

                            String detailCol = details != null ? details : "";

                            historyModel.addRow(new String[] {
                                time,
                                activityLabel,
                                detailCol,
                                ip,
                                status
                            });
                        }
                    }

                    JScrollPane scrollPane = new JScrollPane(historyTable);
                    scrollPane.setBorder(BorderFactory.createEmptyBorder());
                    tablePanel.add(scrollPane, BorderLayout.CENTER);

                    contentPanel.add(tablePanel, BorderLayout.CENTER);
                    historyDialog.add(contentPanel, BorderLayout.CENTER);

                    historyDialog.setVisible(true);

                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(AdminFrame.this,
                        "Lỗi khi tải lịch sử: " + e.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private JPanel createStatCard(String title, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        // Value
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(accentColor);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(valueLabel, BorderLayout.CENTER);

        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(120, 130, 140));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        card.add(titleLabel, BorderLayout.SOUTH);

        return card;
    }

    private void logout() {
        if (heartbeatTimer != null) {
            heartbeatTimer.stop();
        }

    // Reflect logout immediately in local activity feed
    onlineUsers.put(currentUser.getUsername(), false);
    markCurrentUserOfflineInActivityFeed();
        
        // Send logout request
        try {
            Request logoutRequest = new Request("LOGOUT");
            logoutRequest.put("username", currentUser.getUsername());
            networkClient.send(logoutRequest);
        } catch (Exception e) {
            // Ignore logout errors
        } finally {
            if (networkClient != null) {
                try {
                    networkClient.close();
                } catch (Exception ignore) {}
            }
        }
        
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void markCurrentUserOfflineInActivityFeed() {
        if (currentUser == null || currentUser.getUsername() == null) {
            return;
        }

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::markCurrentUserOfflineInActivityFeed);
            return;
        }

        Component comp = findComponentByName(mainContentPanel, "recentActivityList");
        if (comp instanceof JPanel) {
            JPanel activityList = (JPanel) comp;
            for (Component child : activityList.getComponents()) {
                if (child instanceof UserChip) {
                    UserChip chip = (UserChip) child;
                    if (currentUser.getUsername().equalsIgnoreCase(chip.getUsername())) {
                        chip.updateStatus(false, "- vừa đăng xuất");
                    }
                }
            }
            activityList.revalidate();
            activityList.repaint();
        }
    }

    private Component findComponentByName(Container container, String name) {
        if (container == null || name == null) {
            return null;
        }
        for (Component comp : container.getComponents()) {
            if (name.equals(comp.getName())) {
                return comp;
            }
            if (comp instanceof Container) {
                Component nested = findComponentByName((Container) comp, name);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
    
    @Override
    public void dispose() {
        if (heartbeatTimer != null) {
            heartbeatTimer.stop();
        }
        super.dispose();
    }
    
    private void showUserDetailDialog(String username) {
        // Find user by username
        User selectedUser = null;
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                selectedUser = u;
                break;
            }
        }

        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin người dùng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create modern card-style dialog
        JDialog detailDialog = new JDialog(this, "Thông tin chi tiết", true);
        detailDialog.setUndecorated(true); // Remove window decorations for custom styling
        detailDialog.setSize(700, 600);
        detailDialog.setLocationRelativeTo(this);
        detailDialog.setLayout(new BorderLayout());

        // Main container with background
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(new Color(246, 250, 252)); // #F6FAFC
        mainContainer.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Card container with shadow and radius
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Center the card and let it expand
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(cardPanel, BorderLayout.CENTER);
        mainContainer.add(centerWrapper, BorderLayout.CENTER);

        // Gradient header
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(45, 156, 219), // #2D9CDB
                    getWidth(), 0, new Color(59, 130, 246) // #3B82F6
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Subtle shadow at bottom
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fillRect(0, getHeight() - 2, getWidth(), 2);

                g2d.dispose();
            }
        };
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        headerPanel.setPreferredSize(new Dimension(0, 80));

        JLabel titleLabel = new JLabel("Thông tin người dùng");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton closeButton = new JButton("X");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        closeButton.setBackground(new Color(255, 255, 255, 200));
        closeButton.setForeground(new Color(45, 156, 219));
        closeButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setBackground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setBackground(new Color(255, 255, 255, 200));
            }
        });
        closeButton.addActionListener(e -> detailDialog.dispose());
        headerPanel.add(closeButton, BorderLayout.EAST);

        cardPanel.add(headerPanel, BorderLayout.NORTH);

        // Content panel with left-right layout
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // Left column - Avatar and status
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));

        // Circular avatar without border
        final CircularAvatarLabel avatarLabel = new CircularAvatarLabel();
        avatarLabel.setPreferredSize(new Dimension(140, 140));
        avatarLabel.setMaximumSize(new Dimension(140, 140));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(Color.WHITE);

        if (selectedUser.getAvatar() != null && !selectedUser.getAvatar().trim().isEmpty()) {
            ImageIcon icon = ImageCache.getInstance().getImage(selectedUser.getAvatar(), 140, 140, new ImageCache.ImageLoadCallback() {
                @Override
                public void onImageLoaded(ImageIcon loadedIcon) {
                    SwingUtilities.invokeLater(() -> {
                        if (loadedIcon != null) {
                            avatarLabel.setIcon(makeCircularImageIcon(loadedIcon, 140));
                            avatarLabel.setText("");
                        } else {
                            avatarLabel.setText("Lỗi tải ảnh");
                            avatarLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                        }
                    });
                }
            });
            if (icon != null) {
                avatarLabel.setIcon(makeCircularImageIcon(icon, 140));
                avatarLabel.setText("");
            } else {
                avatarLabel.setText("Đang tải...");
                avatarLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            }
        } else {
            avatarLabel.setText("👤");
            avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 70));
        }

        leftPanel.add(avatarLabel);
        leftPanel.add(Box.createVerticalStrut(20));

        // User status badge
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        statusPanel.setOpaque(false);

        String status = selectedUser.getStatus() != null ? selectedUser.getStatus() : "Unknown";
        Color statusColor;
        String statusText;

        switch (status.toLowerCase()) {
            case "active":
                statusColor = new Color(34, 197, 94); // Green
                statusText = "● Hoạt động";
                break;
            case "inactive":
                statusColor = new Color(156, 163, 175); // Gray
                statusText = "● Không hoạt động";
                break;
            case "locked":
                statusColor = new Color(239, 68, 68); // Red
                statusText = "● Đã khóa";
                break;
            default:
                statusColor = new Color(156, 163, 175);
                statusText = "● " + status;
        }

        JLabel statusLabel = new JLabel(statusText);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(statusColor);
        statusPanel.add(statusLabel);

        leftPanel.add(statusPanel);

        // Add vertical glue to push content to top
        leftPanel.add(Box.createVerticalGlue());

        contentPanel.add(leftPanel, BorderLayout.WEST);

        // Right column - User info grid
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 0, 12, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Info fields without icons
        Object[][] infoFields = {
            {"Tên đăng nhập", selectedUser.getUsername()},
            {"Họ và tên", selectedUser.getFullName() != null ? selectedUser.getFullName() : "Chưa cập nhật"},
            {"Email", selectedUser.getEmail() != null ? selectedUser.getEmail() : "Chưa cập nhật"},
            {"Vai trò", selectedUser.getRole() != null ? selectedUser.getRole() : "Chưa cập nhật"},
            {"Ngày tạo", selectedUser.getCreatedAt() != null ? selectedUser.getCreatedAt() : "Chưa cập nhật"}
        };

        for (int i = 0; i < infoFields.length; i++) {
            gbc.gridy = i;

            // Label
            gbc.gridx = 0;
            gbc.weightx = 0.0;
            JLabel label = new JLabel((String)infoFields[i][0] + ":");
            label.setFont(new Font("Segoe UI", Font.BOLD, 14));
            label.setForeground(new Color(75, 85, 99)); // #4B5563
            infoPanel.add(label, gbc);

            // Value
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(12, 20, 12, 0);
            JLabel value = new JLabel((String)infoFields[i][1]);
            value.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            value.setForeground(new Color(17, 24, 39)); // #111827
            infoPanel.add(value, gbc);

            gbc.insets = new Insets(12, 0, 12, 0);
        }

        rightPanel.add(infoPanel, BorderLayout.NORTH);
        // Add vertical glue to match left panel height
        rightPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);

        contentPanel.add(rightPanel, BorderLayout.CENTER);

        cardPanel.add(contentPanel, BorderLayout.CENTER);

        mainContainer.add(centerWrapper, BorderLayout.CENTER);
        detailDialog.add(mainContainer, BorderLayout.CENTER);

        // Make dialog draggable
        Point mousePoint = new Point();
        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mousePoint.x = e.getX();
                mousePoint.y = e.getY();
            }
        });
        headerPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = detailDialog.getLocation();
                detailDialog.setLocation(p.x + e.getX() - mousePoint.x, p.y + e.getY() - mousePoint.y);
            }
        });

        detailDialog.setVisible(true);
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
    private void refreshActivityFeed(JPanel dashboardContent) {
        // Find activity feed panel and refresh it
        findAndRefreshActivityFeed(dashboardContent);
    }
    
    private void findAndRefreshActivityFeed(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                if (scrollPane.getViewport().getView() instanceof JPanel) {
                    JPanel activityList = (JPanel) scrollPane.getViewport().getView();
                    if (activityList.getLayout() instanceof BoxLayout) {
                        loadRecentActivity(activityList);
                        return;
                    }
                }
            } else if (comp instanceof Container) {
                findAndRefreshActivityFeed((Container) comp);
            }
        }
    }
    
    private void refreshPieChart(JPanel container) {
        findAndRefreshPieChart(container);
    }
    
    private void findAndRefreshPieChart(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof PieChartPanel) {
                PieChartPanel pieChart = (PieChartPanel) comp;
                loadStatusDistributionData(pieChart);
                return;
            } else if (comp instanceof Container) {
                findAndRefreshPieChart((Container) comp);
            }
        }
    }
    
    // Custom Pie Chart Panel
    private class PieChartPanel extends JPanel {
        private Map<String, Integer> data = new HashMap<>();
        private Map<String, Color> colors = new HashMap<>();
        private String[] colorPalette = {
            "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", 
            "#06B6D4", "#84CC16", "#F97316", "#EC4899", "#6366F1"
        };
        
        public PieChartPanel() {
            setPreferredSize(new Dimension(400, 250));
            setOpaque(false);
            setLayout(new BorderLayout());
        }
        
        public void setData(Map<String, Integer> newData, Map<String, Color> customColors) {
            this.data = new HashMap<>(newData);
            if (customColors == null) {
                assignColors();
            } else {
                this.colors = new HashMap<>(customColors);
                // Fill in any missing colors with default palette
                int i = 0;
                for (String key : data.keySet()) {
                    if (!colors.containsKey(key)) {
                        Color color = Color.decode(colorPalette[i % colorPalette.length]);
                        colors.put(key, color);
                        i++;
                    }
                }
            }
            repaint();
        }
        
        private void assignColors() {
            colors.clear();
            int i = 0;
            for (String key : data.keySet()) {
                Color color = Color.decode(colorPalette[i % colorPalette.length]);
                colors.put(key, color);
                i++;
            }
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (data.isEmpty()) {
                // Draw placeholder
                g2d.setColor(new Color(120, 130, 140));
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                FontMetrics fm = g2d.getFontMetrics();
                String text = "Đang tải dữ liệu...";
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                g2d.drawString(text, (getWidth() - textWidth) / 2, (getHeight() + textHeight) / 2);
                g2d.dispose();
                return;
            }
            
            // Calculate total
            int total = data.values().stream().mapToInt(Integer::intValue).sum();
            if (total == 0) {
                g2d.setColor(new Color(120, 130, 140));
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                FontMetrics fm = g2d.getFontMetrics();
                String text = "Không có dữ liệu";
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                g2d.drawString(text, (getWidth() - textWidth) / 2, (getHeight() + textHeight) / 2);
                g2d.dispose();
                return;
            }
            
            // Draw pie chart on the left 65%
            int chartWidth = (int) (getWidth() * 0.65);
            int diameter = Math.min(chartWidth - 40, getHeight() - 40);
            int x = 20; // Left margin
            int y = (getHeight() - diameter) / 2;
            
            double currentAngle = 0;
            
            // Calculate angles more precisely to ensure full 360 degrees
            List<Double> angles = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                double percentage = (double) entry.getValue() / total;
                angles.add(percentage * 360);
            }
            
            // Adjust the last angle to fill any rounding gap
            double angleSum = angles.stream().mapToDouble(Double::doubleValue).sum();
            if (!angles.isEmpty()) {
                angles.set(angles.size() - 1, angles.get(angles.size() - 1) + (360 - angleSum));
            }
            
            int i = 0;
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                String key = entry.getKey();
                int value = entry.getValue();
                double arcAngle = angles.get(i);
                
                // Draw pie slice
                g2d.setColor(colors.get(key));
                g2d.fillArc(x, y, diameter, diameter, (int) Math.round(currentAngle), (int) Math.round(arcAngle));
                
                // Draw thin border around slice
                g2d.setColor(new Color(0, 0, 0, 10)); // rgba(0,0,0,0.04)
                g2d.drawArc(x, y, diameter, diameter, (int) Math.round(currentAngle), (int) Math.round(arcAngle));
                
                // Draw percentage label on slice if angle > 15 degrees
                if (arcAngle > 15) {
                    double labelAngle = currentAngle + arcAngle / 2;
                    double labelRadius = diameter * 0.6;
                    int labelX = x + diameter / 2 + (int) (Math.cos(Math.toRadians(labelAngle - 90)) * labelRadius);
                    int labelY = y + diameter / 2 + (int) (Math.sin(Math.toRadians(labelAngle - 90)) * labelRadius);
                    
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    String percentText = String.format("%.1f%%", (double) value / total * 100);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(percentText);
                    int textHeight = fm.getHeight();
                    g2d.drawString(percentText, labelX - textWidth / 2, labelY + textHeight / 4);
                }
                
                currentAngle += arcAngle;
                i++;
            }
            
            // Draw legend on the right 35%, moved slightly left
            int legendX = chartWidth + 16; // Reduced gap from 24 to 16px to move left
            int legendY = 20;
            int legendWidth = getWidth() - legendX - 10; // Reduced padding to give more space
            
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            FontMetrics fm = g2d.getFontMetrics();
            
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                String key = entry.getKey();
                int value = entry.getValue();
                double percentage = (double) value / total;
                String percentText = String.format("%.1f%%", percentage * 100);
                
                // Format display name
                String displayName = key;
                if ("admin_active".equals(key)) displayName = "Admin (ACTIVE)";
                else if ("user_active".equals(key)) displayName = "User (ACTIVE)";
                else if ("admin_locked".equals(key)) displayName = "Admin (LOCKED)";
                else if ("user_locked".equals(key)) displayName = "User (LOCKED)";
                
                // Draw color box
                g2d.setColor(colors.get(key));
                g2d.fillRect(legendX, legendY, 14, 14);
                g2d.setColor(new Color(0, 0, 0, 10));
                g2d.drawRect(legendX, legendY, 14, 14);
                
                // Draw text
                g2d.setColor(Color.BLACK);
                String legendText = displayName + " - " + percentText;
                
                // Wrap text if too long
                int maxWidth = legendWidth - 20;
                if (fm.stringWidth(legendText) > maxWidth) {
                    // Split into two lines
                    String[] words = legendText.split(" ");
                    String line1 = "";
                    String line2 = "";
                    for (String word : words) {
                        if (fm.stringWidth(line1 + " " + word) < maxWidth) {
                            line1 += (line1.isEmpty() ? "" : " ") + word;
                        } else {
                            line2 += (line2.isEmpty() ? "" : " ") + word;
                        }
                    }
                    g2d.drawString(line1, legendX + 20, legendY + 12);
                    if (!line2.isEmpty()) {
                        g2d.drawString(line2, legendX + 20, legendY + 28);
                        legendY += 16; // Extra space for second line
                    }
                } else {
                    g2d.drawString(legendText, legendX + 20, legendY + 12);
                }
                
                legendY += 24; // Line height
            }
            
            g2d.dispose();
        }
    }

    // Modern table cell renderer with avatar support and chips
    private class ModernTableCellRenderer extends JPanel implements TableCellRenderer {
        private final JLabel avatarLabel;
        private final JLabel accountPrimaryLabel;
        private final JLabel accountSecondaryLabel;
        private final JPanel userInfoPanel;
        private final JLabel contentLabel;
        private final JPanel chipPanel;
        
        public ModernTableCellRenderer() {
            setLayout(new BorderLayout());
            setOpaque(true);

            avatarLabel = new CircularAvatarLabel();
            avatarLabel.setPreferredSize(new Dimension(48, 48));
            avatarLabel.setHorizontalAlignment(JLabel.CENTER);
            avatarLabel.setOpaque(false);
            avatarLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            avatarLabel.setForeground(new Color(107, 114, 128));

            accountPrimaryLabel = new JLabel();
            accountPrimaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            accountPrimaryLabel.setForeground(new Color(31, 41, 55));

            accountSecondaryLabel = new JLabel();
            accountSecondaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            accountSecondaryLabel.setForeground(new Color(107, 114, 128));

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.add(accountPrimaryLabel);
            textPanel.add(Box.createVerticalStrut(4));
            textPanel.add(accountSecondaryLabel);

            userInfoPanel = new JPanel(new BorderLayout());
            userInfoPanel.setOpaque(false);
            userInfoPanel.add(textPanel, BorderLayout.CENTER);

            contentLabel = new JLabel();
            contentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            contentLabel.setForeground(new Color(55, 65, 81));
            contentLabel.setHorizontalAlignment(SwingConstants.LEFT);

            chipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            chipPanel.setOpaque(false);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            // Clear components
            removeAll();
            chipPanel.removeAll();
            
            // Set background colors
            Color bgColor = isSelected ? table.getSelectionBackground() : table.getBackground();
            setBackground(bgColor);
            avatarLabel.setBackground(bgColor);
            
            // Optimized padding for better content display
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            
            if (column == 1 && value instanceof String) {
                String avatarUrl = (String) value;
                avatarLabel.setIcon(null);
                avatarLabel.setText("");

                if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                    // Check if image is already cached
                    ImageIcon cachedIcon = ImageCache.getInstance().getImage(avatarUrl, 48, 48, new ImageCache.ImageLoadCallback() {
                        @Override
                        public void onImageLoaded(ImageIcon icon) {
                            SwingUtilities.invokeLater(() -> {
                                if (icon != null) {
                                    avatarLabel.setIcon(icon);
                                    avatarLabel.setText("");
                                } else {
                                    avatarLabel.setText("No Image");
                                    avatarLabel.setIcon(null);
                                }
                                repaint();
                            });
                        }
                    });
                    
                    if (cachedIcon != null) {
                        // Already cached, use immediately
                        avatarLabel.setIcon(cachedIcon);
                        avatarLabel.setText("");
                    } else {
                        // Loading from cache or internet
                        avatarLabel.setText("Loading...");
                    }
                } else {
                    avatarLabel.setText("No Image");
                }

                add(avatarLabel, BorderLayout.CENTER);

            } else if (value instanceof UserDisplayInfo) {
                UserDisplayInfo info = (UserDisplayInfo) value;
                accountPrimaryLabel.setText(info.username != null ? info.username : "");

                if (info.fullName != null && !info.fullName.isEmpty()) {
                    accountSecondaryLabel.setText("@" + info.fullName);
                    accountSecondaryLabel.setVisible(true);
                } else {
                    accountSecondaryLabel.setText("");
                    accountSecondaryLabel.setVisible(false);
                }

                add(userInfoPanel, BorderLayout.CENTER);

            } else if (value instanceof StatusChip) {
                // Status/Role chips
                StatusChip chip = (StatusChip) value;
                
                JLabel chipLabel = new JLabel(chip.text);
                chipLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                chipLabel.setForeground(chip.getTextColor());
                chipLabel.setOpaque(true);
                chipLabel.setBackground(chip.getBackgroundColor());
                chipLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(chip.getTextColor().brighter(), 1, true),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
                chipLabel.setHorizontalAlignment(JLabel.CENTER);
                
                chipPanel.add(chipLabel);
                add(chipPanel, BorderLayout.CENTER);
                
            } else if (value instanceof OnlineStatus) {
                // Online status with dot indicator
                OnlineStatus status = (OnlineStatus) value;
                
                JPanel statusPanel = new JPanel();
                statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
                statusPanel.setOpaque(false);
                
                JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                topPanel.setOpaque(false);
                
                    // Dot indicator
                    JLabel dotLabel = new JLabel("●");
                    dotLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    dotLabel.setForeground(status.isOnline ? new Color(34, 197, 94) : new Color(156, 163, 175));

                    JLabel statusLabel = new JLabel(" " + status.status);
                    statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    statusLabel.setForeground(status.isOnline ? new Color(22, 163, 74) : new Color(107, 114, 128));

                    topPanel.add(dotLabel);
                    topPanel.add(statusLabel);
                    statusPanel.add(topPanel);

                    if (!status.isOnline && status.lastLogin != null && !status.lastLogin.isEmpty()) {
                        JLabel lastLoginLabel = new JLabel(status.lastLogin);
                        lastLoginLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                        lastLoginLabel.setForeground(new Color(156, 163, 175));
                        // Left align the offline time and give a small left margin
                        lastLoginLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                        lastLoginLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));
                        statusPanel.add(lastLoginLabel);
                    }
                
                add(statusPanel, BorderLayout.CENTER);
                
            } else {
                // Default text content
                contentLabel.setText(value != null ? value.toString() : "");
                add(contentLabel, BorderLayout.CENTER);
            }
            
            return this;
        }
    }
    
    // Update Action cell renderer (styled to match main Actions column)
    private class UpdateActionCellRenderer extends JPanel implements TableCellRenderer {
        public JButton updateBtn;
        public JButton resetPasswordBtn;

        public UpdateActionCellRenderer() {
            // Center an inner panel vertically using GridBagLayout
            setLayout(new GridBagLayout());
            setOpaque(true);

            updateBtn = new JButton("Cập nhật");
            updateBtn.setPreferredSize(new Dimension(92, 34));
            updateBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            updateBtn.setBackground(new Color(59, 130, 246)); // Blue primary style
            updateBtn.setForeground(Color.WHITE);
            updateBtn.setFocusPainted(false);
            updateBtn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

            resetPasswordBtn = new JButton("Đặt lại MK");
            resetPasswordBtn.setPreferredSize(new Dimension(78, 34));
            resetPasswordBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            resetPasswordBtn.setBackground(new Color(243, 244, 246));
            resetPasswordBtn.setForeground(new Color(51, 65, 85));
            resetPasswordBtn.setFocusPainted(false);
            resetPasswordBtn.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));

            JPanel inner = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            inner.setOpaque(false);
            inner.add(updateBtn);
            inner.add(resetPasswordBtn);

            add(inner); // GridBagLayout will center this inner panel
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();
            setBackground(bg);

            return this;
        }
    }
    
    // Update Action cell editor
    private class UpdateActionCellEditor extends AbstractCellEditor implements TableCellEditor {
        private UpdateActionCellRenderer renderer;
        private User selectedUser;
        
        public UpdateActionCellEditor() {
            renderer = new UpdateActionCellRenderer();
            
            // Add mouse listeners for update button
            renderer.updateBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (selectedUser != null) {
                        handleUserUpdate(selectedUser);
                    }
                    fireEditingStopped();
                }
            });
            
            // Add mouse listeners for reset password button
            renderer.resetPasswordBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (selectedUser != null) {
                        handlePasswordReset(selectedUser);
                    }
                    fireEditingStopped();
                }
            });
        }
        
        private void handleUserUpdate(User user) {
            // Open user edit dialog
            UserEditDialog dialog = new UserEditDialog((JFrame) SwingUtilities.getWindowAncestor(AdminFrame.this), 
                                                      "Cập nhật thông tin người dùng", user, networkClient);
            dialog.setVisible(true);
            
            if (dialog.isConfirmed()) {
                loadUpdateUsersData(); // Refresh the update table
            }
        }
        
        private void handlePasswordReset(User user) {
            int option = JOptionPane.showConfirmDialog(
                AdminFrame.this,
                "Bạn có chắc chắn muốn đặt lại mật khẩu cho người dùng '" + user.getUsername() + "'?",
                "Xác nhận đặt lại mật khẩu",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            
                if (option == JOptionPane.YES_OPTION) {
                SwingWorker<Response, Void> worker = new SwingWorker<Response, Void>() {
                            @Override
                            protected Response doInBackground() throws Exception {
                                // Prompt admin for new password (on EDT)
                                final String[] provided = new String[1];
                                SwingUtilities.invokeAndWait(() -> {
                                    JPasswordField pwdField = new JPasswordField();
                                    Object[] prompts = {"Nhập mật khẩu mới cho '" + user.getUsername() + "':", pwdField};
                                    int opt = JOptionPane.showConfirmDialog(AdminFrame.this, prompts, "Đặt lại mật khẩu", JOptionPane.OK_CANCEL_OPTION);
                                    if (opt == JOptionPane.OK_OPTION) {
                                        provided[0] = new String(pwdField.getPassword());
                                    } else {
                                        provided[0] = null;
                                    }
                                });

                                if (provided[0] == null || provided[0].isEmpty()) {
                                    // Admin cancelled or provided empty password
                                    return null;
                                }

                                Request request = new Request("ADMIN_RESET_PASSWORD");
                                request.put("requestedBy", currentUser.getUsername());
                                request.put("targetUserId", String.valueOf(user.getId()));
                                request.put("newPassword", provided[0]);

                                Response response = networkClient.send(request);
                                return response;
                            }

                            @Override
                            protected void done() {
                                try {
                                    Response response = get();
                                    if (response == null) {
                                        // User cancelled the prompt
                                        return;
                                    }
                                    if (response != null && response.isSuccess()) {
                                        JOptionPane.showMessageDialog(AdminFrame.this,
                                            "Đã đặt lại mật khẩu thành công cho người '" + user.getUsername() + "'.",
                                            "Thành công",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    } else {
                                        String err = response != null ? response.getMessage() : "Lỗi kết nối";
                                        JOptionPane.showMessageDialog(AdminFrame.this,
                                            "Không thể đặt lại mật khẩu cho người dùng '" + user.getUsername() + "'.\n" + err,
                                            "Lỗi",
                                            JOptionPane.ERROR_MESSAGE);
                                    }
                                } catch (Exception e) {
                                    JOptionPane.showMessageDialog(AdminFrame.this,
                                        "Lỗi khi đặt lại mật khẩu: " + e.getMessage(),
                                        "Lỗi",
                                        JOptionPane.ERROR_MESSAGE);
                                }
                            }
                };
                worker.execute();
            }
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            
            // Get user from the filtered list based on current panel
            // For update panel, we need to get the user from the loaded data
            List<User> currentUsers = getCurrentUsersForUpdatePanel();
            if (row >= 0 && row < currentUsers.size()) {
                selectedUser = currentUsers.get(row);
            } else {
                selectedUser = null;
            }
            
            return renderer.getTableCellRendererComponent(table, value, isSelected, true, row, column);
        }
        
        @Override
        public Object getCellEditorValue() {
            return selectedUser;
        }
    }
    
    private List<User> getCurrentUsersForUpdatePanel() {
        // Return the current users list - this should be maintained for the update panel
        return users; // Using the main users list for now
    }

    // Modern action cell editor with primary action and dropdown menu
    private class ActionCellEditor extends AbstractCellEditor implements TableCellEditor {
        private ActionCellRenderer renderer;
        private User selectedUser;
        
        public ActionCellEditor() {
            renderer = new ActionCellRenderer();
            
            // Add mouse listeners for the modern buttons
            renderer.primaryBtn.addActionListener(e -> {
                if (selectedUser != null) {
                    handleLockToggle(selectedUser);
                }
                fireEditingStopped();
            });
            
            renderer.menuBtn.addActionListener(e -> {
                if (selectedUser != null) {
                    showUserHistory(selectedUser);
                }
                fireEditingStopped();
            });
        }
        
        
        // Helper methods for actions (history and lock handled elsewhere)
        
        private void handleLockToggle(User user) {
            boolean isLocked = "LOCKED".equals(user.getStatus());
            String newStatus = isLocked ? "ACTIVE" : "LOCKED";
            String action = isLocked ? "mở khóa" : "khóa";
            
            int confirm = JOptionPane.showConfirmDialog(AdminFrame.this,
                "Bạn có chắc muốn " + action + " người dùng: " + user.getUsername() + "?",
                "Xác nhận " + action,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    setStatusAndProgress("Đang " + action + " người dùng...", true);
                    
                    Request request = new Request("ADMIN_SET_STATUS");
                    request.put("id", String.valueOf(user.getId()));
                    request.put("status", newStatus);
                    request.put("requestedBy", AdminFrame.this.currentUser.getUsername());
                    
                    Response response = networkClient.send(request);
                    return response != null && response.isSuccess();
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            JOptionPane.showMessageDialog(AdminFrame.this, 
                                "Đã " + action + " người dùng thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                            loadUsers();
                        } else {
                            JOptionPane.showMessageDialog(AdminFrame.this, 
                                "Không thể " + action + " người dùng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                        setStatusAndProgress("Sẵn sàng", false);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(AdminFrame.this, 
                            "Lỗi khi " + action + " người dùng: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                        setStatusAndProgress("Lỗi", false);
                    }
                }
            };
            worker.execute();
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            
            if (row >= 0 && row < filteredUsers.size()) {
                selectedUser = filteredUsers.get(row);
            } else {
                selectedUser = null;
            }
            
            return renderer.getTableCellRendererComponent(table, value, isSelected, true, row, column);
        }
        
        @Override
        public Object getCellEditorValue() {
            return selectedUser;
        }
    }
    
    // Export removed from toolbar - placeholder removed
    
    // Helper classes for modern table rendering
    private static class UserDisplayInfo {
        final String username;
        final String fullName;
        
        UserDisplayInfo(String username, String fullName) {
            this.username = username;
            this.fullName = fullName;
        }
    }
    
    private static class StatusChip {
        enum Type { SUCCESS, DANGER, INFO, SECONDARY }
        
        final String text;
        final Type type;
        
        StatusChip(String text, Type type) {
            this.text = text;
            this.type = type;
        }
        
        Color getBackgroundColor() {
            switch (type) {
                case SUCCESS: return new Color(220, 252, 231);
                case DANGER: return new Color(254, 226, 226);
                case INFO: return new Color(219, 234, 254);
                default: return new Color(243, 244, 246);
            }
        }
        
        Color getTextColor() {
            switch (type) {
                case SUCCESS: return new Color(22, 163, 74);
                case DANGER: return new Color(220, 38, 38);
                case INFO: return new Color(37, 99, 235);
                default: return new Color(107, 114, 128);
            }
        }
    }
    
    private static class OnlineStatus {
        final boolean isOnline;
        final String status;
        final String lastLogin;
        
        OnlineStatus(boolean isOnline, String status, String lastLogin) {
            this.isOnline = isOnline;
            this.status = status;
            this.lastLogin = lastLogin;
        }
    }
    
    // UserChip - nhỏ gọn hiển thị avatar + tên + trạng thái thời gian
    private class UserChip extends JPanel {
        private static final int AVATAR_DISPLAY_SIZE = 40;
        private static final int AVATAR_FETCH_SIZE = 512;
        private static final int BADGE_SIZE = 12;
        private static final int BADGE_INSET = 2;
        private static final int CHIP_HEIGHT = 52;

        private String fullName;
        private String username;
        private String avatarUrl;
        private boolean isOnline;
        private String timeInfo;

        private JLabel nameLabel;
        private JLabel statusLabel;
        private JPanel statusBadge;

        UserChip(String fullName, String username, String avatarUrl, boolean isOnline, String timeInfo) {
            this.fullName = fullName != null ? fullName : username;
            this.username = username != null ? username : "";
            this.avatarUrl = avatarUrl;
            this.isOnline = isOnline;
            this.timeInfo = timeInfo != null ? timeInfo : "";
            initUI();
        }

        private void initUI() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, CHIP_HEIGHT));

            JPanel avatarHolder = new JPanel(null);
            avatarHolder.setOpaque(false);
            avatarHolder.setPreferredSize(new Dimension(AVATAR_DISPLAY_SIZE, AVATAR_DISPLAY_SIZE));
            avatarHolder.setMinimumSize(new Dimension(AVATAR_DISPLAY_SIZE, AVATAR_DISPLAY_SIZE));
            avatarHolder.setMaximumSize(new Dimension(AVATAR_DISPLAY_SIZE, AVATAR_DISPLAY_SIZE));

            CircularAvatarLabel avatarLabel = new CircularAvatarLabel();
            avatarLabel.setBounds(0, 0, AVATAR_DISPLAY_SIZE, AVATAR_DISPLAY_SIZE);
            avatarLabel.setPreferredSize(new Dimension(AVATAR_DISPLAY_SIZE, AVATAR_DISPLAY_SIZE));
            avatarLabel.setSize(new Dimension(AVATAR_DISPLAY_SIZE, AVATAR_DISPLAY_SIZE));
            avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
            avatarLabel.setVerticalAlignment(SwingConstants.CENTER);

            ImageIcon placeholder = new ImageIcon(createFallbackImage(getInitials(), AVATAR_DISPLAY_SIZE));
            avatarLabel.setIcon(placeholder);

            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                try {
                    ImageIcon cached = ImageCache.getInstance().getImage(avatarUrl, AVATAR_FETCH_SIZE, AVATAR_FETCH_SIZE, icon -> {
                        if (icon != null) {
                            ImageIcon scaled = AdminFrame.this.createHighQualityCircularIcon(icon, AVATAR_DISPLAY_SIZE);
                            avatarLabel.setIcon(scaled);
                            avatarLabel.revalidate();
                            avatarLabel.repaint();
                        }
                    });
                    if (cached != null) {
                        ImageIcon scaled = AdminFrame.this.createHighQualityCircularIcon(cached, AVATAR_DISPLAY_SIZE);
                        avatarLabel.setIcon(scaled);
                    }
                } catch (Exception ex) {
                    // Keep placeholder if failed
                }
            }

            avatarHolder.add(avatarLabel);

            statusBadge = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setColor(isOnline ? new Color(46, 204, 113) : new Color(160, 160, 160));
                    g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                    g2.setStroke(new BasicStroke(2f));
                    g2.setColor(Color.WHITE);
                    g2.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
                    g2.dispose();
                }
            };
            statusBadge.setOpaque(false);
            int badgePosition = AVATAR_DISPLAY_SIZE - BADGE_SIZE - BADGE_INSET;
            statusBadge.setBounds(badgePosition, badgePosition, BADGE_SIZE, BADGE_SIZE);
            avatarHolder.add(statusBadge);

            add(avatarHolder);
            add(Box.createRigidArea(new Dimension(12, 0)));

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);

            nameLabel = new JLabel(this.fullName != null && !this.fullName.isEmpty() ? this.fullName : this.username);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            nameLabel.setForeground(new Color(32, 41, 56));

            statusLabel = new JLabel();
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            textPanel.add(nameLabel);
            textPanel.add(statusLabel);

            add(textPanel);

            refreshStatusLabel();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(new Color(245, 247, 250));
                    setOpaque(true);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setOpaque(false);
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    showUserDetailDialog(username);
                }
            });
        }

        String getUsername() {
            return username;
        }

        void updateStatus(boolean online, String newTimeInfo) {
            this.isOnline = online;
            this.timeInfo = newTimeInfo != null ? newTimeInfo : "";
            refreshStatusLabel();
            if (statusBadge != null) {
                statusBadge.repaint();
            }
        }

        private void refreshStatusLabel() {
            String base = isOnline ? "Đang trực tuyến" : "Ngoại tuyến";
            String extra = timeInfo != null ? timeInfo.trim() : "";
            if (!extra.isEmpty()) {
                if (extra.startsWith("-")) {
                    base = base + " " + extra;
                } else {
                    base = base + " - " + extra;
                }
            }
            statusLabel.setText(base);
            statusLabel.setForeground(isOnline ? new Color(46, 204, 113) : new Color(130, 130, 130));
        }

        private String getInitials() {
            if (fullName == null || fullName.trim().isEmpty()) return "?";
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
            String a = parts[parts.length - 1].substring(0, 1);
            String b = parts[0].substring(0, 1);
            return (a + b).toUpperCase();
        }

        // Reuse the fallback renderer from AdminFrame
        private BufferedImage createFallbackImage(String text, int size) {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            
            // Enhanced rendering hints for maximum image quality
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            
            g2.setColor(new Color(216, 27, 96)); // #D81B60
            g2.fillOval(0, 0, size, size);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(12, size / 3)));
            FontMetrics fm = g2.getFontMetrics();
            int strWidth = fm.stringWidth(text);
            int strHeight = fm.getAscent();
            g2.drawString(text, (size - strWidth) / 2, (size + strHeight) / 2 - 3);
            g2.dispose();
            return img;
        }
    }

    // Helper methods for styling components in create user panel
    private void styleInputField(JTextField field) {
        field.setPreferredSize(new Dimension(300, 40)); // Height 40px
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1), // #d1d5db
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setBackground(Color.WHITE);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(45, 156, 219), 2), // #2D9CDB
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
    }

    private void styleInputField(JPasswordField field) {
        field.setPreferredSize(new Dimension(300, 40));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setBackground(Color.WHITE);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(45, 156, 219), 2),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setPreferredSize(new Dimension(300, 40));
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        combo.setBackground(Color.WHITE);
        combo.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                combo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(45, 156, 219), 2),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                combo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
    }

    private void stylePrimaryButton(JButton button) {
        button.setPreferredSize(new Dimension(140, 40));
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(new Color(45, 156, 219)); // #2D9CDB
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(30, 136, 229)); // Darker on hover
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(45, 156, 219));
            }
        });
    }

    private void styleSecondaryButton(JButton button) {
        button.setPreferredSize(new Dimension(100, 40));
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(new Color(243, 244, 246)); // #f3f4f6
        button.setForeground(new Color(75, 85, 99)); // #4b5563
        button.setBorder(BorderFactory.createLineBorder(new Color(156, 163, 175), 1)); // #9ca3af
        button.setFocusPainted(false);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(229, 231, 235)); // Lighter on hover
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(243, 244, 246));
            }
        });
    }
}