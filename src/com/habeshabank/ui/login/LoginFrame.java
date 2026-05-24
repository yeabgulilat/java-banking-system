package com.habeshabank.ui.login;

import com.habeshabank.exception.AuthenticationException;
import com.habeshabank.exception.BankingException;
import com.habeshabank.model.UserSession;
import com.habeshabank.service.AuthService;
import com.habeshabank.ui.components.*;
import com.habeshabank.ui.dashboard.MainFrame;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * The application's login/authentication screen.
 * Left half: branding panel with Ethiopian pattern art.
 * Right half: login form.
 */
public class LoginFrame extends JFrame {

    private HabeshaTextField   accountField;
    private HabeshaPasswordField passwordField;
    private GoldButton         loginButton;
    private JLabel             statusLabel;

    public LoginFrame() {
        setTitle("Habesha Digital Banking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 580);
        setMinimumSize(new Dimension(800, 520));
        setLocationRelativeTo(null);
        setResizable(true);

        initComponents();
    }

    private void initComponents() {
        JPanel root = new JPanel(new GridLayout(1, 2, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(HabeshaTheme.BLACK_DEEP);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(true);

        root.add(buildBrandingPanel());
        root.add(buildFormPanel());

        setContentPane(root);
    }

    // ── Left: Branding ────────────────────────────────────────────────────────

    private JPanel buildBrandingPanel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintBrandingBackground((Graphics2D) g);
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BorderLayout());

        // Top and bottom pattern strips
        EthiopianPatternPanel topBar    = new EthiopianPatternPanel(EthiopianPatternPanel.Orientation.HORIZONTAL, 28);
        EthiopianPatternPanel bottomBar = new EthiopianPatternPanel(EthiopianPatternPanel.Orientation.HORIZONTAL, 28);

        // Center content
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));

        center.add(Box.createVerticalGlue());

        // Logo symbol
        JLabel logoIcon = new JLabel("✦", SwingConstants.CENTER);
        logoIcon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 52));
        logoIcon.setForeground(HabeshaTheme.GOLD_PRIMARY);
        logoIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(logoIcon);

        center.add(Box.createVerticalStrut(20));

        // Bank name (Amharic-style lettering simulation with Palatino)
        JLabel nameLabel = new JLabel("ሀበሻ ባንክ", SwingConstants.CENTER);
        nameLabel.setFont(new Font("Nyala", Font.BOLD, 30));
        nameLabel.setForeground(HabeshaTheme.GOLD_PRIMARY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(nameLabel);

        JLabel nameLatin = new JLabel("HABESHA BANK", SwingConstants.CENTER);
        nameLatin.setFont(HabeshaTheme.FONT_TITLE);
        nameLatin.setForeground(HabeshaTheme.CREAM_LIGHT);
        nameLatin.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(nameLatin);

        center.add(Box.createVerticalStrut(16));

        JSeparator sep = new JSeparator();
        sep.setForeground(HabeshaTheme.GOLD_MUTED);
        sep.setMaximumSize(new Dimension(180, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(sep);

        center.add(Box.createVerticalStrut(16));

        JLabel tagline = new JLabel("<html><div style='text-align:center;'>Digital Banking for<br/>the Ethiopian Future</div></html>", SwingConstants.CENTER);
        tagline.setFont(HabeshaTheme.FONT_SUBHEAD);
        tagline.setForeground(HabeshaTheme.CREAM_DIM);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(tagline);

        center.add(Box.createVerticalStrut(40));

        // Feature bullets
        String[] features = { "✓  Instant Transfers", "✓  Digital Equb", "✓  Iddir Contributions", "✓  PDF Receipts" };
        for (String f : features) {
            JLabel fl = new JLabel(f);
            fl.setFont(HabeshaTheme.FONT_SMALL);
            fl.setForeground(HabeshaTheme.CREAM_DIM);
            fl.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(fl);
            center.add(Box.createVerticalStrut(6));
        }

        center.add(Box.createVerticalGlue());

        panel.add(topBar,    BorderLayout.NORTH);
        panel.add(center,    BorderLayout.CENTER);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private void paintBrandingBackground(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() / 2;
        int h = getHeight();

        // Deep gradient
        GradientPaint gp = new GradientPaint(0, 0, HabeshaTheme.BLACK_RICH,
                0, h, new Color(0x0E, 0x0B, 0x05));
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        // Subtle radial highlight in center
        g2.setColor(new Color(0xC9, 0xA0, 0x2A, 12));
        g2.fillOval(w / 2 - 160, h / 2 - 160, 320, 320);

        // Right border separator
        g2.setColor(HabeshaTheme.GOLD_MUTED);
        g2.fillRect(w - 1, 0, 1, h);
    }

    // ── Right: Login Form ─────────────────────────────────────────────────────

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(HabeshaTheme.BLACK_RICH);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        outer.setOpaque(false);
        outer.setLayout(new GridBagLayout());

        JPanel form = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D bg = new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.setColor(HabeshaTheme.BLACK_CARD);
                g2.fill(bg);
                g2.setColor(HabeshaTheme.BLACK_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(bg);
                g2.dispose();
            }
        };
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(36, 40, 36, 40));
        form.setPreferredSize(new Dimension(360, 440));
        form.setMaximumSize(new Dimension(360, 440));

        // Title
        JLabel title = new JLabel("Welcome Back");
        title.setFont(HabeshaTheme.FONT_TITLE);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(title);

        form.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Sign in to your account");
        subtitle.setFont(HabeshaTheme.FONT_SMALL);
        subtitle.setForeground(HabeshaTheme.CREAM_DIM);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(subtitle);

        form.add(Box.createVerticalStrut(32));

        // Account number
        form.add(fieldLabel("Account Number / Username"));
        form.add(Box.createVerticalStrut(6));
        accountField = new HabeshaTextField("e.g. ETH-2024-00142", 20);
        accountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        accountField.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(accountField);

        form.add(Box.createVerticalStrut(20));

        // Password
        form.add(fieldLabel("Password / PIN"));
        form.add(Box.createVerticalStrut(6));
        passwordField = new HabeshaPasswordField("Enter your password", 20);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(passwordField);

        form.add(Box.createVerticalStrut(8));

        // Forgot password link
        JLabel forgotLabel = new JLabel("Forgot Password?");
        forgotLabel.setFont(HabeshaTheme.FONT_SMALL);
        forgotLabel.setForeground(HabeshaTheme.GOLD_MUTED);
        forgotLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        forgotLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(LoginFrame.this,
                        "Please visit your nearest Habesha Bank branch.",
                        "Password Recovery", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        form.add(forgotLabel);

        form.add(Box.createVerticalStrut(28));

        // Login button
        loginButton = new GoldButton("Sign In");
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.addActionListener(e -> attemptLogin());
        form.add(loginButton);

        form.add(Box.createVerticalStrut(14));

        // Status label (errors / info)
        statusLabel = new JLabel(" ");
        statusLabel.setFont(HabeshaTheme.FONT_SMALL);
        statusLabel.setForeground(HabeshaTheme.RED_DANGER);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(statusLabel);

        form.add(Box.createVerticalGlue());

        // Demo hint
        JLabel demoHint = new JLabel("Demo: ETH-2026-00142  /  demo1234");
        demoHint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        demoHint.setForeground(HabeshaTheme.BLACK_BORDER);
        demoHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(demoHint);

        // Enter key triggers login
        passwordField.addActionListener(e -> attemptLogin());
        accountField.addActionListener(e -> passwordField.requestFocusInWindow());

        outer.add(form);
        return outer;
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(HabeshaTheme.FONT_SMALL);
        lbl.setForeground(HabeshaTheme.CREAM_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    // ── Login Logic ───────────────────────────────────────────────────────────

    private void attemptLogin() {
        String account  = accountField.getText().trim();
        char[] password = passwordField.getPassword();

        if (account.isEmpty()) {
            statusLabel.setText("Please enter your account number.");
            return;
        }
        if (password.length == 0) {
            statusLabel.setText("Please enter your password.");
            return;
        }

        loginButton.setEnabled(false);
        statusLabel.setForeground(HabeshaTheme.GOLD_MUTED);
        statusLabel.setText("Authenticating…");

        // Snapshot password chars before the Timer closure captures them;
        // AuthService will wipe them from memory after verification.
        final char[] passwordSnapshot = password.clone();

        Timer timer = new Timer(600, e -> {
            try {
                // Phase 2: real auth through AuthService
                AuthService.getInstance().authenticate(account, passwordSnapshot);

            } catch (BankingException authEx) {
                // Auth failed — restore UI and show error
                loginButton.setEnabled(true);
                statusLabel.setForeground(HabeshaTheme.RED_DANGER);
                statusLabel.setText(authEx.getMessage());
                return;
            }

            // Auth succeeded — open dashboard
            dispose();
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
        timer.setRepeats(false);
        timer.start();
    }
}