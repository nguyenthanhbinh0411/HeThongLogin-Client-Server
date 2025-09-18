package com.myapp.client;

import com.myapp.common.Request;
import com.myapp.common.Response;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.ArrayList;

// Import FlatLaf - Uncomment when FlatLaf is available in classpath
// import com.formdev.flatlaf.FlatLightLaf;
// import com.formdev.flatlaf.ui.FlatRoundBorder;

public class ClientMain {
    private static NetworkClient net;
    private static JFrame loginFrame;
    private static JFrame adminFrame;
    private static JTable usersTable;
    private static JButton lockButton;
    private static Timer heartbeatTimer;
    private static boolean isLoggedIn = false;
    private static List<Object[]> originalUserData = new ArrayList<>();
    
    // ================ MODERN DESIGN SYSTEM ================
    
    // Modern Color Palette
    private static final Color BG_PRIMARY = new Color(246, 248, 250);      // #F6F8FA
    private static final Color SURFACE = Color.WHITE;                       // #FFFFFF
    private static final Color PRIMARY = new Color(25, 118, 210);           // #1976D2
    private static final Color SUCCESS = new Color(46, 125, 50);            // #2E7D32
    private static final Color WARNING = new Color(255, 179, 0);            // #FFB300
    private static final Color DANGER = new Color(211, 47, 47);             // #D32F2F
    private static final Color TEXT_PRIMARY = new Color(17, 24, 39);        // #111827
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128);   // #6B7280
    private static final Color DIVIDER = new Color(230, 233, 238);          // #E6E9EE
    private static final Color HOVER_BG = new Color(237, 246, 255);         // #EDF6FF
    private static final Color SELECTED_BG = new Color(251, 252, 254);      // #FBFCFE
    
    // Modern Typography
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_SUBHEADING = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 13);
    
    // Modern Spacing & Dimensions
    private static final int SPACING_XS = 4;
    private static final int SPACING_SM = 8;
    private static final int SPACING_MD = 16;
    private static final int SPACING_LG = 24;
    private static final int SPACING_XL = 32;
    private static final int BORDER_RADIUS = 8;
    private static final int BORDER_RADIUS_SM = 6;
    private static final int BUTTON_HEIGHT = 40;
    private static final int TABLE_ROW_HEIGHT = 48;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Try to setup FlatLaf first, fallback to Nimbus if not available
                setupFlatLaf();
            } catch (Exception e) {
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                } catch (Exception ex) {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }
            }
            
            // Setup modern UI styling
            setupModernUI();
            
            try {
                net = new NetworkClient("localhost", 5555);
                net.connect();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Không kết nối được server: " + e.getMessage());
                return;
            }
            buildLogin();
        });
    }

    // Small helper to simplify DocumentListener lambdas
    private static class SimpleDocListener implements javax.swing.event.DocumentListener {
        private final Runnable r;
        SimpleDocListener(Runnable r) { this.r = r; }
        public void insertUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
    }
    
    private static void setupFlatLaf() throws Exception {
        // Try to load FlatLaf using reflection to avoid compile errors
        try {
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            java.lang.reflect.Method setupMethod = flatLafClass.getMethod("setup");
            boolean success = (Boolean) setupMethod.invoke(null);
            
            if (success) {
                System.out.println("FlatLaf loaded successfully!");
                // Configure FlatLaf specific properties
                UIManager.put("Component.arc", 8);
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.focusWidth", 0);
                UIManager.put("Button.focusWidth", 0);
                UIManager.put("TextComponent.arc", 6);
                UIManager.put("ComboBox.arc", 6);
            }
        } catch (ClassNotFoundException e) {
            System.out.println("FlatLaf not found in classpath, using fallback L&F");
            throw new Exception("FlatLaf not available");
        }
    }
    
    private static void setupModernUI() {
        // Setup modern UI defaults for better button rendering
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.focusWidth", 0);
        UIManager.put("Button.focusWidth", 0);
        UIManager.put("Component.borderWidth", 1);
        UIManager.put("Table.selectionBackground", new Color(237, 246, 255));
        UIManager.put("Table.selectionForeground", new Color(25, 118, 210));
        UIManager.put("Table.gridColor", new Color(230, 233, 238));
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
    }

    private static void buildLogin() {
        loginFrame = new JFrame("Đăng nhập - Hệ thống quản lý người dùng");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.getContentPane().setBackground(BG_PRIMARY);

        // Modern title with enhanced styling
        JLabel title = new JLabel("Chào mừng đến với hệ thống quản lý", SwingConstants.CENTER);
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(SPACING_XL, 0, SPACING_LG, 0));

        // Modern form panel with card-like styling
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(SPACING_XL, SPACING_XL*2, SPACING_XL, SPACING_XL*2),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIVIDER, 1),
                new EmptyBorder(SPACING_XL, SPACING_XL, SPACING_XL, SPACING_XL)
            )
        ));
        form.setBackground(SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(SPACING_SM, SPACING_SM, SPACING_SM, SPACING_SM);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Modern labels with improved styling
        JLabel lUser = new JLabel("Tên đăng nhập:");
        lUser.setFont(FONT_BODY);
        lUser.setForeground(TEXT_PRIMARY);
        JTextField tfUser = new JTextField(20);
        tfUser.setFont(FONT_BODY);
        tfUser.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DIVIDER, 1),
            new EmptyBorder(SPACING_SM, SPACING_MD, SPACING_SM, SPACING_MD)
        ));
        
        JLabel lPass = new JLabel("Mật khẩu:");
        lPass.setFont(FONT_BODY);
        lPass.setForeground(TEXT_PRIMARY);
        JPasswordField pf = new JPasswordField(20);
        pf.setFont(FONT_BODY);
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DIVIDER, 1),
            new EmptyBorder(SPACING_SM, SPACING_MD, SPACING_SM, SPACING_MD)
        ));

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        form.add(lUser, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        form.add(tfUser, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        form.add(lPass, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        form.add(pf, gbc);

        JCheckBox cbShow = new JCheckBox("Hiển thị mật khẩu");
        cbShow.setFont(FONT_BODY);
        cbShow.setBackground(SURFACE);
        cbShow.setForeground(TEXT_SECONDARY);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        form.add(cbShow, gbc);

        // buttons panel with modern styling
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        btns.setBackground(SURFACE);
        JButton btnLogin = createModernButton("Đăng nhập", PRIMARY, SURFACE);
        JButton btnRegister = createModernButton("Đăng ký", TEXT_SECONDARY, SURFACE);
        btns.add(btnLogin);
        btns.add(btnRegister);

    loginFrame.getContentPane().add(title, BorderLayout.NORTH);
    loginFrame.getContentPane().add(form, BorderLayout.CENTER);
    loginFrame.getContentPane().add(btns, BorderLayout.SOUTH);
    // Use pack for responsive sizing and set a reasonable minimum for small windows
    loginFrame.pack();
    loginFrame.setMinimumSize(new Dimension(380, 260));
    loginFrame.setLocationRelativeTo(null);
    loginFrame.setVisible(true);

        // show/hide password
        cbShow.addActionListener(ae -> {
            if (cbShow.isSelected()) pf.setEchoChar((char) 0);
            else pf.setEchoChar('\u2022');
        });

        // helper to perform login
        Runnable doLogin = () -> {
            String u = tfUser.getText().trim();
            String pw = new String(pf.getPassword());
            if (u.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(loginFrame, "Nhập đầy đủ");
                return;
            }
            try {
                Request req = new Request("LOGIN");
                req.put("username", u);
                req.put("password", pw);
                Response resp = net.send(req);
                if (!resp.isSuccess()) {
                    JOptionPane.showMessageDialog(loginFrame, "Login fail: " + resp.getMessage());
                    return;
                }
                String role = resp.getData().get("role");
                isLoggedIn = true;
                startHeartbeat();
                if ("ADMIN".equalsIgnoreCase(role)) {
                    SwingUtilities.invokeLater(() -> buildAdminFrame(resp.getData()));
                } else {
                    SwingUtilities.invokeLater(() -> buildUserFrame(resp.getData()));
                }
                loginFrame.dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(loginFrame, "Error: " + ex.getMessage());
            }
        };

        // action bindings
        btnLogin.addActionListener(ev -> doLogin.run());
        pf.addActionListener(ev -> doLogin.run());
        tfUser.addActionListener(ev -> doLogin.run());

        // register flow - use custom dialog with inline validation
        btnRegister.addActionListener(ev -> {
            JDialog dialog = new JDialog(loginFrame, "Đăng ký tài khoản mới", true);
            dialog.setLayout(new BorderLayout());
            JPanel regForm = new JPanel(new GridBagLayout());
            regForm.setBorder(new EmptyBorder(12, 12, 12, 12));

            GridBagConstraints regGbc = new GridBagConstraints();
            regGbc.fill = GridBagConstraints.HORIZONTAL;
            regGbc.insets = new Insets(6, 6, 2, 6);

            JTextField tfNewUser = new JTextField(20);
            JPasswordField pfNew = new JPasswordField(20);
            JPasswordField pfConfirm = new JPasswordField(20);
            JTextField tfFull = new JTextField(20);
            JTextField tfEmail = new JTextField(20);
            String[] roles = {"NGƯỜI DÙNG"};
            JComboBox<String> cbRole = new JComboBox<>(roles);

            // validation labels
            JLabel vUser = new JLabel(" "); vUser.setForeground(DANGER);
            JLabel vPass = new JLabel(" "); vPass.setForeground(DANGER);
            JLabel vConfirm = new JLabel(" "); vConfirm.setForeground(DANGER);
            JLabel vFull = new JLabel(" "); vFull.setForeground(DANGER);
            JLabel vEmail = new JLabel(" "); vEmail.setForeground(DANGER);

            int y = 0;
            regGbc.gridx = 0; regGbc.gridy = y; regForm.add(new JLabel("Tên đăng nhập:"), regGbc);
            regGbc.gridx = 1; regGbc.gridy = y++; regForm.add(tfNewUser, regGbc);
            regGbc.gridx = 1; regGbc.gridy = y++; regForm.add(vUser, regGbc);

            regGbc.gridx = 0; regGbc.gridy = y; regForm.add(new JLabel("Mật khẩu:"), regGbc);
            regGbc.gridx = 1; regGbc.gridy = y++; regForm.add(pfNew, regGbc);
            regGbc.gridx = 1; regGbc.gridy = y++; regForm.add(vPass, regGbc);

            regGbc.gridx = 0; regGbc.gridy = y; regForm.add(new JLabel("Xác nhận mật khẩu:"), regGbc);
            regGbc.gridx = 1; regGbc.gridy = y++; regForm.add(pfConfirm, regGbc);
            regGbc.gridx = 1; regGbc.gridy = y++; regForm.add(vConfirm, regGbc);

            regGbc.gridx = 0; regGbc.gridy = y; regForm.add(new JLabel("Họ và tên:"), regGbc);
            regGbc.gridx = 1; regGbc.gridy = y++; regForm.add(tfFull, regGbc);
            regGbc.gridx = 1; regGbc.gridy = y++; regForm.add(vFull, regGbc);

            regGbc.gridx = 0; regGbc.gridy = y; regForm.add(new JLabel("Email:"), regGbc);
            regGbc.gridx = 1; regGbc.gridy = y++; regForm.add(tfEmail, regGbc);
            regGbc.gridx = 1; regGbc.gridy = y++; regForm.add(vEmail, regGbc);

            regGbc.gridx = 0; regGbc.gridy = y; regForm.add(new JLabel("Vai trò:"), regGbc);
            regGbc.gridx = 1; regGbc.gridy = y++; regForm.add(cbRole, regGbc);

            // helper validation function
            Runnable runValidation = () -> {
                String un = tfNewUser.getText().trim();
                String pw = new String(pfNew.getPassword());
                String confirmPw = new String(pfConfirm.getPassword());
                String full = tfFull.getText().trim();
                String email = tfEmail.getText().trim();

                vUser.setText(un.length() < 3 ? "Tên đăng nhập phải có ít nhất 3 ký tự" : " ");
                vPass.setText(pw.length() < 6 ? "Mật khẩu phải có ít nhất 6 ký tự" : " ");
                vConfirm.setText(!pw.equals(confirmPw) ? "Mật khẩu xác nhận không khớp" : " ");
                vFull.setText(full.isEmpty() ? "Họ và tên không được để trống" : " ");
                vEmail.setText(!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") ? "Email không hợp lệ" : " ");
            };

            // validate on typing
            tfNewUser.getDocument().addDocumentListener(new SimpleDocListener(runValidation));
            pfNew.getDocument().addDocumentListener(new SimpleDocListener(runValidation));
            pfConfirm.getDocument().addDocumentListener(new SimpleDocListener(runValidation));
            tfFull.getDocument().addDocumentListener(new SimpleDocListener(runValidation));
            tfEmail.getDocument().addDocumentListener(new SimpleDocListener(runValidation));

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton okBtn = createModernButton("Đăng ký", PRIMARY, SURFACE);
            JButton cancelBtn = createModernButton("Hủy", TEXT_SECONDARY, SURFACE);
            footer.add(cancelBtn);
            footer.add(okBtn);

            cancelBtn.addActionListener(e2 -> dialog.dispose());
            okBtn.addActionListener(e2 -> {
                runValidation.run();
                // final validation check
                if (!vUser.getText().trim().isEmpty() && !vUser.getText().equals(" ")) return;
                if (!vPass.getText().trim().isEmpty() && !vPass.getText().equals(" ")) return;
                if (!vConfirm.getText().trim().isEmpty() && !vConfirm.getText().equals(" ")) return;
                if (!vFull.getText().trim().isEmpty() && !vFull.getText().equals(" ")) return;
                if (!vEmail.getText().trim().isEmpty() && !vEmail.getText().equals(" ")) return;

                // Send register request
                try {
                    Request r = new Request("REGISTER");
                    r.put("username", tfNewUser.getText().trim());
                    r.put("password", new String(pfNew.getPassword()));
                    r.put("fullName", tfFull.getText().trim());
                    r.put("email", tfEmail.getText().trim());
                    r.put("role", mapRoleToEnglish((String) cbRole.getSelectedItem()));
                    Response resp = net.send(r);
                    JOptionPane.showMessageDialog(loginFrame, resp.getMessage());
                    if (resp.isSuccess()) {
                        tfUser.setText(tfNewUser.getText().trim());
                        pf.requestFocusInWindow();
                        dialog.dispose();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(loginFrame, ex.getMessage());
                }
            });

            dialog.add(regForm, BorderLayout.CENTER);
            dialog.add(footer, BorderLayout.SOUTH);
            dialog.pack();
            dialog.setLocationRelativeTo(loginFrame);
            runValidation.run();
            dialog.setVisible(true);
        });
    }

    private static void buildUserFrame(java.util.Map<String, String> data) {
        JFrame f = new JFrame("Bảng điều khiển người dùng - " + data.get("username"));
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().setBackground(BG_PRIMARY);

        // Modern title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(PRIMARY);
        JLabel titleLabel = new JLabel("Thông tin tài khoản", SwingConstants.CENTER);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(SURFACE);
        titleLabel.setBorder(new EmptyBorder(SPACING_LG, 0, SPACING_LG, 0));
        titlePanel.add(titleLabel);

        // Modern card panel
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(SPACING_XL, SPACING_XL*2, SPACING_XL, SPACING_XL*2),
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DIVIDER, 1),
                    new EmptyBorder(SPACING_XL, SPACING_XL, SPACING_XL, SPACING_XL)
                )
        ));
        card.setBackground(SURFACE);
        card.setOpaque(true);

        // Avatar section
        JLabel avatar = new JLabel();
        avatar.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatar.setPreferredSize(new Dimension(80, 80));
        card.add(avatar);
        card.add(Box.createVerticalStrut(SPACING_LG));

        // User info panel with modern styling
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(SPACING_SM, SPACING_SM, SPACING_SM, SPACING_SM);
        gbc.anchor = GridBagConstraints.WEST;

        // Username
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblUserTitle = new JLabel("Tên đăng nhập:");
        lblUserTitle.setFont(FONT_BODY.deriveFont(Font.BOLD));
        lblUserTitle.setForeground(TEXT_PRIMARY);
        infoPanel.add(lblUserTitle, gbc);
        
        gbc.gridx = 1;
        JLabel lblUser = new JLabel(data.get("username"));
        lblUser.setFont(FONT_BODY);
        lblUser.setForeground(PRIMARY);
        infoPanel.add(lblUser, gbc);

        // Full Name
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel lblFullTitle = new JLabel("Họ và tên:");
        lblFullTitle.setFont(FONT_BODY.deriveFont(Font.BOLD));
        lblFullTitle.setForeground(TEXT_PRIMARY);
        infoPanel.add(lblFullTitle, gbc);
        
        gbc.gridx = 1;
        JLabel lblFull = new JLabel(data.get("fullName"));
        lblFull.setFont(FONT_BODY);
        lblFull.setForeground(TEXT_PRIMARY);
        infoPanel.add(lblFull, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel lblEmailTitle = new JLabel("Email:");
        lblEmailTitle.setFont(FONT_BODY.deriveFont(Font.BOLD));
        lblEmailTitle.setForeground(TEXT_PRIMARY);
        infoPanel.add(lblEmailTitle, gbc);
        
        gbc.gridx = 1;
        JLabel lblEmail = new JLabel(data.get("email"));
        lblEmail.setFont(FONT_BODY);
        lblEmail.setForeground(TEXT_PRIMARY);
        infoPanel.add(lblEmail, gbc);

        // Role
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel lblRoleTitle = new JLabel("Vai trò:");
        lblRoleTitle.setFont(FONT_BODY.deriveFont(Font.BOLD));
        lblRoleTitle.setForeground(TEXT_PRIMARY);
        infoPanel.add(lblRoleTitle, gbc);
        
        gbc.gridx = 1;
        JLabel lblRole = new JLabel(data.get("role"));
        lblRole.setFont(FONT_BODY);
        lblRole.setForeground(SUCCESS);
        infoPanel.add(lblRole, gbc);

        card.add(infoPanel);
        card.add(Box.createVerticalStrut(SPACING_XL));

        // Modern action buttons panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, SPACING_MD, 0));
        actionPanel.setBackground(SURFACE);
        
        JButton btnEditProfile = createModernButton("Sửa thông tin", PRIMARY, SURFACE);
        btnEditProfile.addActionListener(e -> showEditProfileDialog(data, lblFull, lblEmail));
        
        JButton btnChangePassword = createModernButton("Đổi mật khẩu", WARNING, SURFACE);
        btnChangePassword.addActionListener(e -> showChangePasswordDialog());
        
        actionPanel.add(btnEditProfile);
        actionPanel.add(btnChangePassword);
        card.add(actionPanel);
        card.add(Box.createVerticalStrut(SPACING_LG));

        JButton btnLogout = createModernButton("Đăng xuất", DANGER, SURFACE);
        btnLogout.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogout.addActionListener(e -> {
            stopHeartbeat();
            f.dispose();
            SwingUtilities.invokeLater(ClientMain::buildLogin);
        });
        card.add(btnLogout);

    f.getContentPane().add(titlePanel, BorderLayout.NORTH);
    f.getContentPane().add(card, BorderLayout.CENTER);
    // Use pack for responsive sizing and a smaller minimum so it fits on small screens
    f.pack();
    f.setMinimumSize(new Dimension(420, 420));
    f.setLocationRelativeTo(null);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                stopHeartbeat();
            }
        });
        f.setVisible(true);
    }

    private static void buildAdminFrame(java.util.Map<String, String> data) {
        adminFrame = new JFrame();
        adminFrame.setTitle("Admin Dashboard - User Management System");
        adminFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        adminFrame.setSize(1400, 900);
        adminFrame.setLocationRelativeTo(null);
        
        // Main container with modern background
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(BG_PRIMARY);
        
        // Create modern header
        JPanel headerPanel = createModernHeader(data);
        
        // Create content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BG_PRIMARY);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(SPACING_LG, SPACING_XL, SPACING_LG, SPACING_XL));
        
        // Create toolbar section
        JPanel toolbarSection = createModernToolbar();
        
        // Create search and filters section
        JPanel searchSection = createSearchAndFilters();
        
        // Create main content area (table)
        JPanel tableSection = createModernTableSection();
        
        // Assemble content
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(BG_PRIMARY);
        topSection.add(toolbarSection, BorderLayout.NORTH);
        topSection.add(searchSection, BorderLayout.SOUTH);
        
        contentPanel.add(topSection, BorderLayout.NORTH);
        contentPanel.add(tableSection, BorderLayout.CENTER);
        
        // Assemble main frame
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(contentPanel, BorderLayout.CENTER);
        
        adminFrame.add(mainContainer);
        adminFrame.setVisible(true);
        
        // Load initial data
        refreshUserList();
    }

    // ================ MODERN UI COMPONENTS ================
    
    private static JPanel createModernHeader(java.util.Map<String, String> data) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(SURFACE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER));
        headerPanel.setPreferredSize(new Dimension(0, 70));
        
        // Left side - Logo and title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, SPACING_LG, SPACING_MD));
        leftPanel.setBackground(SURFACE);
        
        // App logo/icon
        JLabel logoLabel = new JLabel("APP");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        
        // Title
        JLabel titleLabel = new JLabel("Admin Dashboard");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT_PRIMARY);
        
        leftPanel.add(logoLabel);
        leftPanel.add(titleLabel);
        
        // Right side - User info and actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, SPACING_LG, SPACING_MD));
        rightPanel.setBackground(SURFACE);
        
        // User greeting
        JLabel userLabel = new JLabel("Xin chào, " + data.get("username"));
        userLabel.setFont(FONT_BODY);
        userLabel.setForeground(TEXT_SECONDARY);
        
        // User avatar (circle)
        JLabel avatarLabel = new JLabel("USER");
        avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        avatarLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DIVIDER, 2),
            BorderFactory.createEmptyBorder(SPACING_SM, SPACING_SM, SPACING_SM, SPACING_SM)
        ));
        
        // Logout button  
        JButton logoutButton = createModernButton("Đăng xuất", DANGER, SURFACE);
        logoutButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                adminFrame,
                "Bạn có chắc chắn muốn đăng xuất?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION
            );
            if (choice == JOptionPane.YES_OPTION) {
                adminFrame.dispose();
                buildLogin();
            }
        });
        
        rightPanel.add(userLabel);
        rightPanel.add(logoutButton);
        
        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private static JPanel createModernToolbar() {
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBackground(BG_PRIMARY);
        toolbarPanel.setBorder(BorderFactory.createEmptyBorder(SPACING_LG, 0, SPACING_MD, 0));
        
        // Primary actions (left)
        JPanel primaryActions = new JPanel(new FlowLayout(FlowLayout.LEFT, SPACING_MD, 0));
        primaryActions.setBackground(BG_PRIMARY);
        
        JButton refreshBtn = createModernButton("Tải lại", TEXT_SECONDARY, SURFACE);
        JButton createBtn = createModernButton("Tạo người dùng", PRIMARY, SURFACE);
        JButton editBtn = createModernButton("Chỉnh sửa", SUCCESS, SURFACE);
    JButton lockBtn = createModernButton("Khóa/Mở", WARNING, SURFACE);
        
        // Add event listeners
        refreshBtn.addActionListener(e -> refreshUserList());
        createBtn.addActionListener(e -> showCreateUserDialog(adminFrame, FONT_BODY));
        editBtn.addActionListener(e -> showEditUserDialog(adminFrame, usersTable, FONT_BODY));
        lockBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row >= 0) {
                toggleUserLock(adminFrame, usersTable);
                refreshUserList();
                updateLockButtonText();
            } else {
                JOptionPane.showMessageDialog(adminFrame, "Vui lòng chọn một tài khoản để khóa/mở.", "Chọn tài khoản", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        primaryActions.add(refreshBtn);
        primaryActions.add(createBtn);
        primaryActions.add(editBtn);
        primaryActions.add(lockBtn);
        
        // System actions (right)
        JPanel systemActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, SPACING_MD, 0));
        systemActions.setBackground(BG_PRIMARY);
        
        JButton logsBtn = createModernButton("Nhật ký", TEXT_SECONDARY, SURFACE);
        
        // Add event listeners
        logsBtn.addActionListener(e -> showAuditDialog(adminFrame));
        
        systemActions.add(logsBtn);
        
        toolbarPanel.add(primaryActions, BorderLayout.WEST);
        toolbarPanel.add(systemActions, BorderLayout.EAST);
        
        return toolbarPanel;
    }
    
    private static JPanel createSearchAndFilters() {
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(BG_PRIMARY);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, SPACING_LG, 0));
        
        // Search box section
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchBox.setBackground(BG_PRIMARY);
        
        JTextField searchField = new JTextField(30);
        searchField.setFont(FONT_BODY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DIVIDER, 1),
            BorderFactory.createEmptyBorder(SPACING_SM, SPACING_MD, SPACING_SM, SPACING_MD)
        ));
        searchField.setPreferredSize(new Dimension(300, BUTTON_HEIGHT));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm theo tên, email, username...");
        
        JLabel searchIcon = new JLabel("Search:");
        searchIcon.setFont(FONT_BODY);
        searchIcon.setBorder(BorderFactory.createEmptyBorder(0, SPACING_SM, 0, 0));
        
        searchBox.add(searchIcon);
        searchBox.add(searchField);
        
        // Filters and actions section
        JPanel filtersPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, SPACING_SM, 0));
        filtersPanel.setBackground(BG_PRIMARY);
        
        // Filter dropdowns
        String[] roles = {"Tất cả vai trò", "Người dùng", "Quản trị viên"};
        JComboBox<String> roleFilter = new JComboBox<>(roles);
        roleFilter.setFont(FONT_SMALL);
        roleFilter.setPreferredSize(new Dimension(150, BUTTON_HEIGHT));
        
        String[] statuses = {"Tất cả trạng thái", "Hoạt động", "Bị khóa"};
        JComboBox<String> statusFilter = new JComboBox<>(statuses);
        statusFilter.setFont(FONT_SMALL);
        statusFilter.setPreferredSize(new Dimension(150, BUTTON_HEIGHT));
        
        String[] onlineStatus = {"Tất cả", "Trực tuyến", "Ngoại tuyến"};
        JComboBox<String> onlineFilter = new JComboBox<>(onlineStatus);
        onlineFilter.setFont(FONT_SMALL);
        onlineFilter.setPreferredSize(new Dimension(120, BUTTON_HEIGHT));
        
        // Export CSV button
        
        // Clear filters button
        JButton clearBtn = createModernButton("Xóa lọc", TEXT_SECONDARY, Color.WHITE);
        clearBtn.setPreferredSize(new Dimension(110, BUTTON_HEIGHT));
        
        filtersPanel.add(new JLabel("Lọc: "));
        filtersPanel.add(roleFilter);
        filtersPanel.add(statusFilter);
        filtersPanel.add(onlineFilter);
        filtersPanel.add(clearBtn);
        
        // Add filtering functionality
        setupFilteringLogic(searchField, roleFilter, statusFilter, onlineFilter, clearBtn);
        
        // Add export functionality
        
        searchPanel.add(searchBox, BorderLayout.WEST);
        searchPanel.add(filtersPanel, BorderLayout.EAST);
        
        return searchPanel;
    }
    
    private static void setupFilteringLogic(JTextField searchField, JComboBox<String> roleFilter, 
                                          JComboBox<String> statusFilter, JComboBox<String> onlineFilter,
                                          JButton clearBtn) {
        // Search field listener
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(searchField, roleFilter, statusFilter, onlineFilter); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(searchField, roleFilter, statusFilter, onlineFilter); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(searchField, roleFilter, statusFilter, onlineFilter); }
        });
        
        // Filter dropdown listeners
        roleFilter.addActionListener(e -> applyFilters(searchField, roleFilter, statusFilter, onlineFilter));
        statusFilter.addActionListener(e -> applyFilters(searchField, roleFilter, statusFilter, onlineFilter));
        onlineFilter.addActionListener(e -> applyFilters(searchField, roleFilter, statusFilter, onlineFilter));
        
        // Clear filters button
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            roleFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            onlineFilter.setSelectedIndex(0);
            refreshUserList(); // Reload original data
        });
    }
    
    private static void applyFilters(JTextField searchField, JComboBox<String> roleFilter,
                                   JComboBox<String> statusFilter, JComboBox<String> onlineFilter) {
        if (usersTable == null) return;
        
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedRole = (String) roleFilter.getSelectedItem();
        String selectedStatus = (String) statusFilter.getSelectedItem();
        String selectedOnline = (String) onlineFilter.getSelectedItem();
        
        DefaultTableModel model = (DefaultTableModel) usersTable.getModel();
        
        // Get current data from table if originalData is empty
        if (originalUserData.isEmpty()) {
            for (int i = 0; i < model.getRowCount(); i++) {
                Object[] row = new Object[model.getColumnCount()];
                for (int j = 0; j < model.getColumnCount(); j++) {
                    row[j] = model.getValueAt(i, j);
                }
                originalUserData.add(row);
            }
        }
        
        // Clear current table
        model.setRowCount(0);
        
        // Apply filters
        for (Object[] row : originalUserData) {
            boolean matches = true;
            
            // Search filter - now search in username, fullName, and email
            if (!searchText.isEmpty()) {
                String username = (row[1] != null ? row[1].toString().toLowerCase() : "");
                String fullName = (row[2] != null ? row[2].toString().toLowerCase() : "");
                String email = (row[3] != null ? row[3].toString().toLowerCase() : "");
                String role = (row[4] != null ? row[4].toString().toLowerCase() : "");
                if (!username.contains(searchText) && !fullName.contains(searchText) && 
                    !email.contains(searchText) && !role.contains(searchText)) {
                    matches = false;
                }
            }
            
            // Role filter - now column 4
            if (matches && selectedRole != null && !selectedRole.equals("Tất cả vai trò")) {
                String expectedRole = selectedRole.equals("Người dùng") ? "NGƯỜI DÙNG" : "QUẢN TRỊ VIÊN";
                if (!expectedRole.equals(row[4])) {
                    matches = false;
                }
            }
            
            // Status filter - now column 5
            if (matches && selectedStatus != null && !selectedStatus.equals("Tất cả trạng thái")) {
                String expectedStatus = selectedStatus.equals("Hoạt động") ? "HOẠT ĐỘNG" : "BỊ KHÓA";
                if (!expectedStatus.equals(row[5])) {
                    matches = false;
                }
            }
            
            // Online filter - now column 6
            if (matches && selectedOnline != null && !selectedOnline.equals("Tất cả")) {
                String expectedOnline = selectedOnline.equals("Trực tuyến") ? "TRỰC TUYẾN" : "NGOẠI TUYẾN";
                if (!expectedOnline.equals(row[6])) {
                    matches = false;
                }
            }
            
            if (matches) {
                model.addRow(row);
            }
        }
    }
    
    private static JPanel createModernTableSection() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(SURFACE);
        tablePanel.setBorder(BorderFactory.createLineBorder(DIVIDER, 1));
        
        // Create table
        String[] columns = {"ID", "Tên đăng nhập", "Họ và tên", "Email", "Vai trò", "Trạng thái", "Trực tuyến", "Thao tác"};
        DefaultTableModel tableModel = new DefaultTableModel(new Object[0][], columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        usersTable = new JTable(tableModel);
        usersTable.setFont(FONT_BODY);
        usersTable.setRowHeight(TABLE_ROW_HEIGHT);
        usersTable.setShowGrid(false);
        usersTable.setIntercellSpacing(new Dimension(0, 1));
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usersTable.setSelectionBackground(HOVER_BG);
        usersTable.setSelectionForeground(TEXT_PRIMARY);
        
        // Modern table header
        usersTable.getTableHeader().setFont(FONT_LABEL);
        usersTable.getTableHeader().setBackground(SURFACE);
        usersTable.getTableHeader().setForeground(TEXT_PRIMARY);
        usersTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER));
        usersTable.getTableHeader().setPreferredSize(new Dimension(0, 50));
        
        // Setup modern cell renderers
        setupModernTableRenderers();
        
        JScrollPane scrollPane = new JScrollPane(usersTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(SURFACE);
        
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        
        return tablePanel;
    }
    
    private static void setupModernTableRenderers() {
        // Default renderer with zebra striping and locked-row highlight
        usersTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Determine if this row represents a locked account (column 5 - status)
                boolean locked = false;
                try {
                    Object statusObj = table.getValueAt(row, 5);
                    if (statusObj != null && "BỊ KHÓA".equals(statusObj.toString())) {
                        locked = true;
                    }
                } catch (Exception ex) {
                    // ignore out-of-bounds or other issues
                }

                if (isSelected) {
                    c.setBackground(HOVER_BG);
                    c.setForeground(TEXT_PRIMARY);
                } else if (locked) {
                    // Highlight entire locked row with a soft red background
                    c.setBackground(new Color(255, 220, 220));
                    c.setForeground(TEXT_PRIMARY);
                } else {
                    c.setBackground(row % 2 == 0 ? SURFACE : SELECTED_BG);
                    c.setForeground(TEXT_PRIMARY);
                }

                setBorder(BorderFactory.createEmptyBorder(SPACING_SM, SPACING_MD, SPACING_SM, SPACING_MD));
                return c;
            }
        });
        
        // Role column renderer with badges (column 4)
        usersTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (value != null) {
                    String role = value.toString();
                    if ("QUẢN TRỊ VIÊN".equals(role)) {
                        setText(role);
                        setForeground(isSelected ? TEXT_PRIMARY : PRIMARY);
                    } else {
                        setText(role);
                        setForeground(isSelected ? TEXT_PRIMARY : TEXT_SECONDARY);
                    }
                }
                
                setBackground(isSelected ? HOVER_BG : (row % 2 == 0 ? SURFACE : SELECTED_BG));
                setBorder(BorderFactory.createEmptyBorder(SPACING_SM, SPACING_MD, SPACING_SM, SPACING_MD));
                return c;
            }
        });
        
        // Status column renderer with colored badges (column 5)
        usersTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String status = value.toString();
                    setText(status);
                    if ("BỊ KHÓA".equals(status)) {
                        setForeground(isSelected ? TEXT_PRIMARY : DANGER);
                        c.setBackground(new Color(255, 220, 220)); // đỏ nhạt cho cả hàng
                    } else {
                        setForeground(isSelected ? TEXT_PRIMARY : SUCCESS);
                        c.setBackground(isSelected ? HOVER_BG : (row % 2 == 0 ? SURFACE : SELECTED_BG));
                    }
                }
                setBorder(BorderFactory.createEmptyBorder(SPACING_SM, SPACING_MD, SPACING_SM, SPACING_MD));
                return c;
            }
        });
        
        // Online status column renderer (column 6)
        usersTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (value != null) {
                    String onlineStatus = value.toString();
                    if ("TRỰC TUYẾN".equals(onlineStatus)) {
                        setText(onlineStatus);
                        setForeground(isSelected ? TEXT_PRIMARY : SUCCESS);
                    } else {
                        setText(onlineStatus);
                        setForeground(isSelected ? TEXT_PRIMARY : TEXT_SECONDARY);
                    }
                }
                
                setBackground(isSelected ? HOVER_BG : (row % 2 == 0 ? SURFACE : SELECTED_BG));
                setBorder(BorderFactory.createEmptyBorder(SPACING_SM, SPACING_MD, SPACING_SM, SPACING_MD));
                return c;
            }
        });

        // Update lock button text when selection changes
        usersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateLockButtonText();
            }
        });
    }

    private static void updateLockButtonText() {
        if (lockButton == null || usersTable == null) return;
        int row = usersTable.getSelectedRow();
        if (row < 0) {
            lockButton.setText("Khóa/Mở");
            lockButton.setEnabled(false);
            return;
        }
        lockButton.setEnabled(true);
        Object statusObj = usersTable.getValueAt(row, 5); // Status is now column 5
        String status = statusObj != null ? statusObj.toString() : "";
        if ("BỊ KHÓA".equals(status)) {
            lockButton.setText("Mở khóa");
        } else {
            lockButton.setText("Khóa");
        }
    }
    
    private static JButton createModernButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Modern rounded rectangle background
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Modern shadow effect
                if (getModel().isPressed()) {
                    g2.setColor(new Color(0, 0, 0, 30));
                    g2.fillRoundRect(2, 2, getWidth()-2, getHeight()-2, 12, 12);
                }
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setFont(FONT_BODY);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setPreferredSize(new Dimension(text.length() * 8 + 40, BUTTON_HEIGHT));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Enhanced border with rounded corners
        button.setBorder(BorderFactory.createEmptyBorder(SPACING_SM, SPACING_MD, SPACING_SM, SPACING_MD));
        
        // Enhanced hover effects with smooth color transitions
        button.addMouseListener(new MouseAdapter() {
            private final Color originalBg = bgColor;
            private final Color hoverBg = new Color(
                Math.min(255, Math.max(0, bgColor.getRed() + (bgColor.equals(Color.WHITE) ? -30 : 20))),
                Math.min(255, Math.max(0, bgColor.getGreen() + (bgColor.equals(Color.WHITE) ? -30 : 20))),
                Math.min(255, Math.max(0, bgColor.getBlue() + (bgColor.equals(Color.WHITE) ? -30 : 20)))
            );
            private final Color pressedBg = new Color(
                Math.max(0, bgColor.getRed() - 40),
                Math.max(0, bgColor.getGreen() - 40),
                Math.max(0, bgColor.getBlue() - 40)
            );
            
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
                button.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalBg);
                button.repaint();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressedBg);
                button.repaint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(hoverBg);
                button.repaint();
            }
        });
        
        return button;
    }

    private static void refreshUserList() {
        // Implementation to refresh the users table
        if (usersTable == null) return;
        
        try {
            Request r = new Request("ADMIN_LIST_USERS");
            Response resp = net.send(r);
            if (!resp.isSuccess()) {
                JOptionPane.showMessageDialog(adminFrame, resp.getMessage());
                return;
            }
            
            String usersCsv = resp.getData().get("users");
            StringTokenizer st = new StringTokenizer(usersCsv, ";");
            java.util.List<Object[]> rows = new java.util.ArrayList<>();
            while (st.hasMoreTokens()) {
                String[] vals = st.nextToken().split(",");
                if (vals.length >= 6) {
                    // Format: id,username,fullName,email,role,status,onlineStatus
                    String onlineStatus = (vals.length >= 7) ? vals[6] : "OFFLINE";
                    String vietnameseRole = mapRoleToVietnamese(vals[4]);
                    String vietnameseStatus = mapStatusToVietnamese(vals[5]);
                    String vietnameseOnline = mapOnlineToVietnamese(onlineStatus);
                    rows.add(new Object[]{vals[0], vals[1], vals[2], vals[3], vietnameseRole, vietnameseStatus, vietnameseOnline});
                }
            }
            
            // Clear and update original data for filtering
            originalUserData.clear();
            originalUserData.addAll(rows);
            
            Object[][] arr = rows.toArray(new Object[0][]);
            String[] columns = {"ID", "Tên đăng nhập", "Họ và tên", "Email", "Vai trò", "Trạng thái", "Trực tuyến"};
            DefaultTableModel model = new DefaultTableModel(arr, columns) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            usersTable.setModel(model);
            
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(adminFrame, "Lỗi khi tải danh sách người dùng: " + ex.getMessage());
        }
    }
    
    private static String mapStatusToVietnamese(String englishStatus) {
        switch (englishStatus.toUpperCase()) {
            case "ACTIVE": return "HOẠT ĐỘNG";
            case "LOCKED": return "BỊ KHÓA";
            default: return englishStatus;
        }
    }
    
    private static String mapOnlineToVietnamese(String englishOnline) {
        switch (englishOnline.toUpperCase()) {
            case "ONLINE": return "TRỰC TUYẾN";
            case "OFFLINE": return "NGOẠI TUYẾN";
            default: return englishOnline;
        }
    }
    
    private static String validateUserInput(String username, String password, String fullName, String email) {
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

    private static void startHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
        }
        heartbeatTimer = new Timer(true); // daemon timer
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isLoggedIn && net != null) {
                    try {
                        Request pingReq = new Request("PING");
                        net.send(pingReq);
                    } catch (Exception e) {
                        System.out.println("Heartbeat failed: " + e.getMessage());
                        // Could reconnect here if needed
                    }
                }
            }
        }, 30000, 30000); // Send PING every 30 seconds
    }

    private static void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
        isLoggedIn = false;
    }

    private static void showEditProfileDialog(java.util.Map<String, String> currentData, JLabel lblFull, JLabel lblEmail) {
        JDialog dialog = new JDialog((JFrame) null, "Sửa thông tin cá nhân", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblFullName = new JLabel("Họ và tên:");
        JTextField tfFullName = new JTextField(currentData.get("fullName"), 20);
        JLabel lblEmailAddr = new JLabel("Email:");
        JTextField tfEmail = new JTextField(currentData.get("email"), 20);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        form.add(lblFullName, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        form.add(tfFullName, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        form.add(lblEmailAddr, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        form.add(tfEmail, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton btnSave = new JButton("Lưu");
        JButton btnCancel = new JButton("Hủy");

        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnCancel.setBackground(new Color(108, 117, 125));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);

        btnSave.addActionListener(e -> {
            String fullName = tfFullName.getText().trim();
            String email = tfEmail.getText().trim();
            
            if (fullName.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                JOptionPane.showMessageDialog(dialog, "Email không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                Request req = new Request("UPDATE_PROFILE");
                req.put("fullName", fullName);
                req.put("email", email);
                Response resp = net.send(req);
                
                if (resp.isSuccess()) {
                    // Update UI labels
                    lblFull.setText(fullName);
                    lblEmail.setText(email);
                    // Update current data
                    currentData.put("fullName", fullName);
                    currentData.put("email", email);
                    
                    JOptionPane.showMessageDialog(dialog, resp.getMessage(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, resp.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi kết nối: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static void showChangePasswordDialog() {
        JDialog dialog = new JDialog((JFrame) null, "Đổi mật khẩu", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 280);
        dialog.setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblOldPassword = new JLabel("Mật khẩu cũ:");
        JPasswordField pfOldPassword = new JPasswordField(20);
        JLabel lblNewPassword = new JLabel("Mật khẩu mới:");
        JPasswordField pfNewPassword = new JPasswordField(20);
        JLabel lblConfirmPassword = new JLabel("Nhập lại mật khẩu mới:");
        JPasswordField pfConfirmPassword = new JPasswordField(20);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        form.add(lblOldPassword, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        form.add(pfOldPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        form.add(lblNewPassword, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        form.add(pfNewPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        form.add(lblConfirmPassword, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        form.add(pfConfirmPassword, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton btnChange = new JButton("Đổi mật khẩu");
        JButton btnCancel = new JButton("Hủy");

        btnChange.setBackground(new Color(255, 193, 7));
        btnChange.setForeground(new Color(33, 37, 41));
        btnChange.setFocusPainted(false);
        btnCancel.setBackground(new Color(108, 117, 125));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);

        btnChange.addActionListener(e -> {
            String oldPassword = new String(pfOldPassword.getPassword());
            String newPassword = new String(pfNewPassword.getPassword());
            String confirmPassword = new String(pfConfirmPassword.getPassword());
            
            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(dialog, "Mật khẩu mới và xác nhận mật khẩu không khớp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (newPassword.length() < 6) {
                JOptionPane.showMessageDialog(dialog, "Mật khẩu mới phải có ít nhất 6 ký tự!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                Request req = new Request("CHANGE_PASSWORD");
                req.put("oldPassword", oldPassword);
                req.put("newPassword", newPassword);
                Response resp = net.send(req);
                
                if (resp.isSuccess()) {
                    JOptionPane.showMessageDialog(dialog, resp.getMessage(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, resp.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi kết nối: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        buttonPanel.add(btnChange);
        buttonPanel.add(btnCancel);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static void showAuditTable(JFrame parent, String auditsData) {
        JDialog dialog = new JDialog(parent, "Nhật ký hoạt động hệ thống", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(parent);

        // Parse audit data into table model
        String[] lines = auditsData.split("\n");
        String[] columnNames = {"ID", "Người dùng", "Hành động", "Chi tiết", "Thời gian"};
        Object[][] data = new Object[lines.length][5];
        
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) continue;
            String[] parts = lines[i].split("\\|");
            if (parts.length >= 5) {
                data[i][0] = parts[0];  // ID
                data[i][1] = parts[1];  // Username
                data[i][2] = translateAction(parts[2]);  // Action (translated)
                data[i][3] = parts[3];  // Details
                data[i][4] = parts[4];  // Created At
            }
        }

        JTable table = new JTable(data, columnNames);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        table.setRowHeight(25);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        table.getColumnModel().getColumn(1).setPreferredWidth(150);  // User
        table.getColumnModel().getColumn(2).setPreferredWidth(120);  // Action
        table.getColumnModel().getColumn(3).setPreferredWidth(300);  // Details
        table.getColumnModel().getColumn(4).setPreferredWidth(150);  // Time
        
        // Style the header
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(0, 123, 255));
        table.getTableHeader().setForeground(Color.WHITE);
        
        // Alternating row colors
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    if (row % 2 == 0) {
                        c.setBackground(new Color(248, 249, 250));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Danh sách nhật ký hoạt động"));
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton btnClose = new JButton("Đóng");
        btnClose.setBackground(new Color(108, 117, 125));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(e -> dialog.dispose());
        buttonPanel.add(btnClose);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static String translateAction(String action) {
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

    private static String mapRoleToEnglish(String vietnameseRole) {
        switch (vietnameseRole) {
            case "NGƯỜI DÙNG": return "USER";
            case "QUẢN TRỊ VIÊN": return "ADMIN";
            default: return vietnameseRole; // Fallback for any unmapped roles
        }
    }

    private static String mapRoleToVietnamese(String englishRole) {
        switch (englishRole) {
            case "USER": return "NGƯỜI DÙNG";
            case "ADMIN": return "QUẢN TRỊ VIÊN";
            default: return englishRole; // Fallback for any unmapped roles
        }
    }

    // ================ MODERN UI HELPER METHODS ================
    
    private static JPanel createModernHeader(java.util.Map<String, String> data, Font titleFont, Font modernFont, Color primaryBlue, Color dangerRed) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 229, 229)));
        headerPanel.setPreferredSize(new Dimension(0, 70));
        
        // Left side - Title and avatar
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        leftPanel.setBackground(Color.WHITE);
        
        // Admin avatar (circular)
        JLabel avatarLabel = new JLabel("👨‍💼");
        avatarLabel.setFont(new Font("Segoe UI", Font.PLAIN, 28));
        avatarLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JLabel titleLabel = new JLabel("Admin Dashboard");
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(new Color(33, 37, 41));
        
        leftPanel.add(avatarLabel);
        leftPanel.add(titleLabel);
        
        // Right side - User info and logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15));
        rightPanel.setBackground(Color.WHITE);
        
        JLabel userLabel = new JLabel("Xin chào, " + data.get("username"));
        userLabel.setFont(modernFont);
        userLabel.setForeground(new Color(108, 117, 125));
        
        JButton logoutBtn = createModernButton("🚪 Đăng xuất", dangerRed, Color.WHITE, modernFont);
        logoutBtn.setPreferredSize(new Dimension(130, 35));
        
        rightPanel.add(userLabel);
        rightPanel.add(logoutBtn);
        
        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private static JPanel createModernToolbar(Font modernFont, Color primaryBlue, Color primaryGreen, Color primaryOrange, Color primaryPurple, Color dangerRed) {
        JPanel toolbarContainer = new JPanel(new BorderLayout());
        toolbarContainer.setBackground(new Color(248, 249, 250));
        toolbarContainer.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        // Management group
        JPanel managementGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        managementGroup.setBackground(new Color(248, 249, 250));
        managementGroup.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1), 
            "Quản lý người dùng", 
            0, 0, modernFont, new Color(108, 117, 125)
        ));
        
        JButton btnRefresh = createModernButton("🔄 Tải lại", new Color(108, 117, 125), Color.WHITE, modernFont);
        JButton btnCreate = createModernButton("➕ Tạo mới", primaryBlue, Color.WHITE, modernFont);
        JButton btnEdit = createModernButton("✏️ Chỉnh sửa", primaryGreen, Color.WHITE, modernFont);
        JButton btnToggleLock = createModernButton("🔒 Khóa/Mở", primaryOrange, Color.WHITE, modernFont);
        
        btnToggleLock.setEnabled(false); // Initially disabled
        
        managementGroup.add(btnRefresh);
        managementGroup.add(btnCreate);
        managementGroup.add(btnEdit);
        managementGroup.add(btnToggleLock);
        
        // System group
        JPanel systemGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        systemGroup.setBackground(new Color(248, 249, 250));
        systemGroup.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1), 
            "Hệ thống", 
            0, 0, modernFont, new Color(108, 117, 125)
        ));
        
        JButton btnLogs = createModernButton("Nhật ký", primaryPurple, Color.WHITE, modernFont);
        
        systemGroup.add(btnLogs);
        
        toolbarContainer.add(managementGroup, BorderLayout.WEST);
        toolbarContainer.add(systemGroup, BorderLayout.EAST);
        
        return toolbarContainer;
    }
    
    private static JPanel createSearchPanel(Font modernFont, Color borderGray) {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        searchPanel.setBackground(new Color(248, 249, 250));
        
        JLabel searchLabel = new JLabel("🔍 Tìm kiếm:");
        searchLabel.setFont(modernFont);
        
        JTextField searchField = new JTextField(20);
        searchField.setFont(modernFont);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderGray, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.setPreferredSize(new Dimension(250, 35));
        
        searchPanel.add(searchLabel);
        searchPanel.add(Box.createHorizontalStrut(10));
        searchPanel.add(searchField);
        
        return searchPanel;
    }
    
    private static JTable createModernTable(Object[][] data, String[] columns, Font modernFont, Font headerFont, Color primaryBlue) {
        JTable table = new JTable(data, columns);
        table.setRowHeight(45);
        table.setFont(modernFont);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(primaryBlue.getRed(), primaryBlue.getGreen(), primaryBlue.getBlue(), 50));
        table.setSelectionForeground(primaryBlue);
        
        // Modern header styling
        table.getTableHeader().setFont(headerFont);
        table.getTableHeader().setBackground(primaryBlue);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 50));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
        
        // Set column widths
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(80);   // ID
            table.getColumnModel().getColumn(1).setPreferredWidth(200);  // Username
            table.getColumnModel().getColumn(2).setPreferredWidth(150);  // Role
            table.getColumnModel().getColumn(3).setPreferredWidth(150);  // Status
            table.getColumnModel().getColumn(4).setPreferredWidth(150);  // Online
        }
        
        return table;
    }
    
    private static JButton createModernButton(String text, Color bgColor, Color fgColor, Font font) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(140, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add rounded corners and subtle border
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        
        // Add modern hover effect with darker colors and smooth transition
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            private final Color originalBg = bgColor;
            private final Color hoverBg = new Color(
                Math.max(0, bgColor.getRed() - 30),
                Math.max(0, bgColor.getGreen() - 30), 
                Math.max(0, bgColor.getBlue() - 30)
            );
            
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(hoverBg.darker(), 2),
                    BorderFactory.createEmptyBorder(7, 15, 7, 15)
                ));
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(originalBg.darker(), 1),
                    BorderFactory.createEmptyBorder(8, 16, 8, 16)
                ));
            }
            
            public void mousePressed(java.awt.event.MouseEvent evt) {
                Color pressedBg = new Color(
                    Math.max(0, bgColor.getRed() - 50),
                    Math.max(0, bgColor.getGreen() - 50), 
                    Math.max(0, bgColor.getBlue() - 50)
                );
                button.setBackground(pressedBg);
            }
            
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverBg);
            }
        });
        
        return button;
    }
    
    private static void setupAdminEventHandlers(JFrame frame, JTable table, JPanel toolbarPanel, Font modernFont, Color primaryBlue, Color primaryGreen, Color primaryOrange, Color dangerRed) {
        // Get buttons from toolbar groups
        JPanel managementGroup = (JPanel) toolbarPanel.getComponent(0);
        JPanel systemGroup = (JPanel) toolbarPanel.getComponent(1);
        
        JButton btnRefresh = (JButton) managementGroup.getComponent(0);
        JButton btnCreate = (JButton) managementGroup.getComponent(1);
        JButton btnEdit = (JButton) managementGroup.getComponent(2);
        JButton btnToggleLock = (JButton) managementGroup.getComponent(3);
        JButton btnLogs = (JButton) systemGroup.getComponent(0);
        
        // Get logout button from header
        JPanel headerPanel = (JPanel) ((JPanel) frame.getContentPane().getComponent(0)).getComponent(0);
        JPanel rightPanel = (JPanel) headerPanel.getComponent(1);
        JButton btnLogout = (JButton) rightPanel.getComponent(1);
        
        // Refresh button
        btnRefresh.addActionListener(e -> refreshUserList());
        
        // Create user button
        btnCreate.addActionListener(e -> showCreateUserDialog(frame, modernFont));
        
        // Edit user button
        btnEdit.addActionListener(e -> showEditUserDialog(frame, table, modernFont));
        
        // Toggle lock button
        btnToggleLock.addActionListener(e -> toggleUserLock(frame, table));
        
        // Audit logs button
        btnLogs.addActionListener(e -> showAuditDialog(frame));
        
        // Logout button
        btnLogout.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                frame,
                "Bạn có chắc chắn muốn đăng xuất?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION
            );
            if (choice == JOptionPane.YES_OPTION) {
                stopHeartbeat();
                frame.dispose();
                SwingUtilities.invokeLater(ClientMain::buildLogin);
            }
        });
        
        // Table selection listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRow() != -1;
                btnEdit.setEnabled(hasSelection);
                btnToggleLock.setEnabled(hasSelection);
            }
        });
    }
    
    private static void showCreateUserDialog(JFrame parent, Font modernFont) {
        JDialog dialog = new JDialog(parent, "Tạo người dùng mới", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField tfUser = new JTextField(20);
        JPasswordField tfPwd = new JPasswordField(20);
        JPasswordField tfConfirmPwd = new JPasswordField(20);
        JTextField tfFull = new JTextField(20);
        JTextField tfEmail = new JTextField(20);
        String[] roles = {"NGƯỜI DÙNG", "QUẢN TRỊ VIÊN"};
        JComboBox<String> cbRole = new JComboBox<>(roles);
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Tên đăng nhập:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(tfUser, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(tfPwd, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Xác nhận mật khẩu:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(tfConfirmPwd, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Họ và tên:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(tfFull, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(tfEmail, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Vai trò:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(cbRole, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Color.WHITE);
        JButton okButton = new JButton("Tạo");
        JButton cancelButton = new JButton("Hủy");
        
        okButton.addActionListener(e -> {
            try {
                String username = tfUser.getText().trim();
                String pw = new String(tfPwd.getPassword());
                String confirmPw = new String(tfConfirmPwd.getPassword());
                String fullName = tfFull.getText().trim();
                String email = tfEmail.getText().trim();
                
                if (!pw.equals(confirmPw)) {
                    JOptionPane.showMessageDialog(dialog, "Mật khẩu và xác nhận mật khẩu không khớp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String validationError = validateUserInput(username, pw, fullName, email);
                if (validationError != null) {
                    JOptionPane.showMessageDialog(dialog, validationError, "Lỗi xác thực", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                Request r = new Request("ADMIN_CREATE_USER");
                r.put("username", username);
                r.put("password", pw);
                r.put("fullName", fullName);
                r.put("email", email);
                r.put("role", mapRoleToEnglish((String) cbRole.getSelectedItem()));
                Response resp = net.send(r);
                
                JOptionPane.showMessageDialog(dialog, resp.getMessage());
                if (resp.isSuccess()) {
                    dialog.dispose();
                    refreshUserList();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage());
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private static void showEditUserDialog(JFrame parent, JTable table, Font modernFont) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(parent, "Vui lòng chọn người dùng để chỉnh sửa!");
            return;
        }
        
        String id = table.getValueAt(row, 0).toString();
        String currentUsername = table.getValueAt(row, 1).toString();
        
        try {
            Request getUserReq = new Request("ADMIN_GET_USER");
            getUserReq.put("id", id);
            Response getUserResp = net.send(getUserReq);
            
            if (!getUserResp.isSuccess()) {
                JOptionPane.showMessageDialog(parent, "Không thể lấy thông tin người dùng: " + getUserResp.getMessage());
                return;
            }
            
            JDialog dialog = new JDialog(parent, "Chỉnh sửa người dùng - " + currentUsername, true);
            dialog.setSize(450, 350);
            dialog.setLocationRelativeTo(parent);
            
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            panel.setBackground(Color.WHITE);
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 5, 10, 5);
            gbc.anchor = GridBagConstraints.WEST;
            
            JTextField tfFull = new JTextField(getUserResp.getData().get("fullName"), 20);
            JTextField tfEmail = new JTextField(getUserResp.getData().get("email"), 20);
            String[] roles = {"NGƯỜI DÙNG", "QUẢN TRỊ VIÊN"};
            JComboBox<String> cbRole = new JComboBox<>(roles);
            cbRole.setSelectedItem(mapRoleToVietnamese(getUserResp.getData().get("role")));
            JPasswordField tfPwd = new JPasswordField(20);
            JPasswordField tfConfirmPwd = new JPasswordField(20);
            
            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(new JLabel("Tên đăng nhập:"), gbc);
            gbc.gridx = 1;
            JLabel lblUsername = new JLabel(currentUsername);
            lblUsername.setFont(lblUsername.getFont().deriveFont(Font.BOLD));
            panel.add(lblUsername, gbc);
            
            gbc.gridx = 0; gbc.gridy = 1;
            panel.add(new JLabel("Họ và tên:"), gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(tfFull, gbc);
            
            gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
            panel.add(new JLabel("Email:"), gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(tfEmail, gbc);
            
            gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
            panel.add(new JLabel("Vai trò:"), gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(cbRole, gbc);
            
            gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
            panel.add(new JLabel("Mật khẩu mới:"), gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(tfPwd, gbc);
            
            gbc.gridx = 0; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE;
            panel.add(new JLabel("Xác nhận mật khẩu:"), gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(tfConfirmPwd, gbc);
            
            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.setBackground(Color.WHITE);
            JButton okButton = new JButton("Cập nhật");
            JButton cancelButton = new JButton("Hủy");
            
            okButton.addActionListener(e -> {
                try {
                    String password = new String(tfPwd.getPassword()).trim();
                    String confirmPassword = new String(tfConfirmPwd.getPassword()).trim();
                    
                    if (!password.isEmpty() && !password.equals(confirmPassword)) {
                        JOptionPane.showMessageDialog(dialog, "Mật khẩu và xác nhận mật khẩu không khớp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    Request r = new Request("ADMIN_EDIT_USER");
                    r.put("id", id);
                    r.put("fullName", tfFull.getText().trim());
                    r.put("email", tfEmail.getText().trim());
                    r.put("role", mapRoleToEnglish((String) cbRole.getSelectedItem()));
                    if (!password.isEmpty()) {
                        r.put("password", password);
                    }
                    Response resp = net.send(r);
                    
                    JOptionPane.showMessageDialog(dialog, resp.getMessage());
                    if (resp.isSuccess()) {
                        dialog.dispose();
                        refreshUserList();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage());
                }
            });
            
            cancelButton.addActionListener(e -> dialog.dispose());
            
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
            
            gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
            panel.add(buttonPanel, gbc);
            
            dialog.add(panel);
            dialog.setVisible(true);
            
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Lỗi khi chỉnh sửa người dùng: " + ex.getMessage());
        }
    }
    
    private static void toggleUserLock(JFrame parent, JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(parent, "Vui lòng chọn người dùng để thay đổi trạng thái!");
            return;
        }
        
        String id = table.getValueAt(row, 0).toString();
        String username = table.getValueAt(row, 1).toString();
        String currentStatus = table.getValueAt(row, 3).toString();
        String newStatus = "BỊ KHÓA".equals(currentStatus) ? "ACTIVE" : "LOCKED";
        String action = "BỊ KHÓA".equals(currentStatus) ? "mở khóa" : "khóa";
        
        int choice = JOptionPane.showConfirmDialog(
            parent,
            "Bạn có chắc chắn muốn " + action + " tài khoản '" + username + "'?",
            "Xác nhận " + action,
            JOptionPane.YES_NO_OPTION
        );
        
        if (choice == JOptionPane.YES_OPTION) {
            try {
                Request r = new Request("ADMIN_SET_STATUS");
                r.put("id", id);
                r.put("status", newStatus);
                Response resp = net.send(r);
                
                JOptionPane.showMessageDialog(parent, resp.getMessage());
                if (resp.isSuccess()) {
                    refreshUserList();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(parent, "Lỗi: " + ex.getMessage());
            }
        }
    }
    
    private static void showAuditDialog(JFrame parent) {
        try {
            Request r = new Request("GET_AUDITS");
            Response resp = net.send(r);
            if (!resp.isSuccess()) {
                JOptionPane.showMessageDialog(parent, resp.getMessage());
                return;
            }
            
            String auditsData = resp.getData().get("audits");
            showAuditTable(parent, auditsData);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Lỗi khi tải nhật ký: " + ex.getMessage());
        }
    }
    
    private static JButton getRefreshButton(JPanel toolbarPanel) {
        // Helper to get refresh button for auto-refresh
        return (JButton) ((JPanel) toolbarPanel.getComponent(0)).getComponent(0);
    }
}