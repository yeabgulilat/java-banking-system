package com.habeshabank.ui.login;

import com.habeshabank.exception.ValidationException;
import com.habeshabank.service.AuthService;
import com.habeshabank.ui.components.*;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Registration screen — Phase 5.
 *
 * Layout mirrors LoginFrame exactly:
 *   Left half  : same branding panel (Ethiopian pattern + logo + tagline)
 *   Right half : scrollable registration form inside the same rounded card
 *
 * Design rules preserved
 * ──────────────────────
 * • Same window size (900×620, min 800×560)
 * • Same BLACK_DEEP background, same rounded BLACK_CARD form panel
 * • Same GOLD_PRIMARY / CREAM_LIGHT typography hierarchy
 * • EthiopianPatternPanel strips top and bottom of the branding side
 * • Inline field-level error labels (red, one per field) — no popup dialogs
 *   for validation failures, keeping the UX smooth
 * • Success dialog shows the generated account number prominently, then
 *   navigates back to LoginFrame
 *
 * Validation (client-side, before calling AuthService)
 * ─────────────────────────────────────────────────────
 * • Full name   : required, ≥ 2 characters
 * • Username    : required, 4–30 chars, alphanumeric + dots/underscores only
 * • Email       : required, must contain "@" and a "." after "@"
 * • Phone       : optional, if present must start with "+" or digit
 * • Password    : required, ≥ 6 characters
 * • Confirm pwd : must match password exactly
 *
 * AuthService.register() performs its own server-side validation on top of this.
 */
public class RegisterFrame extends JFrame {

    // ── Form fields ───────────────────────────────────────────────────────────
    private HabeshaTextField     fullNameField;
    private HabeshaTextField     usernameField;
    private HabeshaTextField     emailField;
    private HabeshaTextField     phoneField;
    private HabeshaPasswordField passwordField;
    private HabeshaPasswordField confirmField;
    private HabeshaPasswordField pinField;          // Phase 6
    private HabeshaPasswordField confirmPinField;   // Phase 6
    private GoldButton           registerButton;

    // ── Per-field inline error labels (field name → label) ────────────────────
    private final Map<String, JLabel> errorLabels = new LinkedHashMap<>();

    // ── Regex patterns ────────────────────────────────────────────────────────
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._]{4,30}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+\\d][\\d\\s\\-]{6,19}$");

    // ── Constructor ───────────────────────────────────────────────────────────

    public RegisterFrame() {
        setTitle("Create Account — Habesha Digital Banking System");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 640);
        setMinimumSize(new Dimension(800, 560));
        setLocationRelativeTo(null);
        setResizable(true);
        initComponents();
    }

    // ── Root layout ───────────────────────────────────────────────────────────

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

    // ── Left: Branding (identical to LoginFrame) ──────────────────────────────

    private JPanel buildBrandingPanel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintBrandingBackground((Graphics2D) g);
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BorderLayout());

        EthiopianPatternPanel topBar    =
                new EthiopianPatternPanel(EthiopianPatternPanel.Orientation.HORIZONTAL, 28);
        EthiopianPatternPanel bottomBar =
                new EthiopianPatternPanel(EthiopianPatternPanel.Orientation.HORIZONTAL, 28);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));
        center.add(Box.createVerticalGlue());

        JLabel logoIcon = new JLabel("✦", SwingConstants.CENTER);
        logoIcon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 52));
        logoIcon.setForeground(HabeshaTheme.GOLD_PRIMARY);
        logoIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(logoIcon);
        center.add(Box.createVerticalStrut(20));

        JLabel amharic = new JLabel("ሀበሻ ባንክ", SwingConstants.CENTER);
        amharic.setFont(new Font("Nyala", Font.BOLD, 30));
        amharic.setForeground(HabeshaTheme.GOLD_PRIMARY);
        amharic.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(amharic);

        JLabel latin = new JLabel("HABESHA BANK", SwingConstants.CENTER);
        latin.setFont(HabeshaTheme.FONT_TITLE);
        latin.setForeground(HabeshaTheme.CREAM_LIGHT);
        latin.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(latin);
        center.add(Box.createVerticalStrut(16));

        JSeparator sep = new JSeparator();
        sep.setForeground(HabeshaTheme.GOLD_MUTED);
        sep.setMaximumSize(new Dimension(180, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(sep);
        center.add(Box.createVerticalStrut(16));

        JLabel tagline = new JLabel(
                "<html><div style='text-align:center;'>Open your account today<br/>and join the future of<br/>Ethiopian banking.</div></html>",
                SwingConstants.CENTER);
        tagline.setFont(HabeshaTheme.FONT_SUBHEAD);
        tagline.setForeground(HabeshaTheme.CREAM_DIM);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(tagline);
        center.add(Box.createVerticalStrut(40));

        String[] perks = {
                "✓  Free account — no monthly fees",
                "✓  Instant internal transfers",
                "✓  Digital Equb groups",
                "✓  Iddir mutual-aid support",
                "✓  PDF receipts for every transaction"
        };
        for (String p : perks) {
            JLabel lbl = new JLabel(p);
            lbl.setFont(HabeshaTheme.FONT_SMALL);
            lbl.setForeground(HabeshaTheme.CREAM_DIM);
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(lbl);
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
        GradientPaint gp = new GradientPaint(0, 0, HabeshaTheme.BLACK_RICH,
                0, h, new Color(0x0E, 0x0B, 0x05));
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);
        g2.setColor(new Color(0xC9, 0xA0, 0x2A, 12));
        g2.fillOval(w / 2 - 160, h / 2 - 160, 320, 320);
        g2.setColor(HabeshaTheme.GOLD_MUTED);
        g2.fillRect(w - 1, 0, 1, h);
    }

    // ── Right: Registration Form ──────────────────────────────────────────────

    private JPanel buildFormPanel() {
        // Outer panel — dark background, centres the card
        JPanel outer = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(HabeshaTheme.BLACK_RICH);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        outer.setOpaque(false);
        outer.setLayout(new GridBagLayout());

        // Scrollable card — taller than login to fit all 6 fields
        JPanel card = buildCard();
        JScrollPane scroll = new JScrollPane(card);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(380, 560));
        scroll.setMaximumSize(new Dimension(380, 560));
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        outer.add(scroll);
        return outer;
    }

    private JPanel buildCard() {
        JPanel card = new JPanel() {
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
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));

        // ── Header ────────────────────────────────────────────────────────────
        JLabel title = new JLabel("Create Your Account");
        title.setFont(HabeshaTheme.FONT_TITLE);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);

        card.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Join Habesha Bank — it takes less than a minute");
        subtitle.setFont(HabeshaTheme.FONT_SMALL);
        subtitle.setForeground(HabeshaTheme.CREAM_DIM);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);

        card.add(Box.createVerticalStrut(24));

        // ── Gold divider ──────────────────────────────────────────────────────
        JSeparator div = new JSeparator();
        div.setForeground(HabeshaTheme.GOLD_MUTED);
        div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        div.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(div);
        card.add(Box.createVerticalStrut(20));

        // ── Section label: Personal Info ──────────────────────────────────────
        card.add(sectionLabel("PERSONAL INFORMATION"));
        card.add(Box.createVerticalStrut(12));

        // Full Name
        card.add(fieldLabel("Full Name  *"));
        card.add(Box.createVerticalStrut(5));
        fullNameField = new HabeshaTextField("e.g. Tigist Alemu", 24);
        fullNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        fullNameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(fullNameField);
        card.add(inlineError("fullName"));
        card.add(Box.createVerticalStrut(12));

        // Email
        card.add(fieldLabel("Email Address  *"));
        card.add(Box.createVerticalStrut(5));
        emailField = new HabeshaTextField("e.g. tigist@example.com", 24);
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(emailField);
        card.add(inlineError("email"));
        card.add(Box.createVerticalStrut(12));

        // Phone
        card.add(fieldLabel("Phone Number  (optional)"));
        card.add(Box.createVerticalStrut(5));
        phoneField = new HabeshaTextField("e.g. +251911000001", 24);
        phoneField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        phoneField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(phoneField);
        card.add(inlineError("phone"));
        card.add(Box.createVerticalStrut(20));

        // ── Section label: Account Credentials ───────────────────────────────
        card.add(sectionLabel("ACCOUNT CREDENTIALS"));
        card.add(Box.createVerticalStrut(12));

        // Username
        card.add(fieldLabel("Username  *"));
        card.add(Box.createVerticalStrut(5));
        usernameField = new HabeshaTextField("4–30 chars, letters/digits/dots", 24);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(usernameField);
        card.add(inlineError("username"));
        card.add(Box.createVerticalStrut(12));

        // Password
        card.add(fieldLabel("Password  *"));
        card.add(Box.createVerticalStrut(5));
        passwordField = new HabeshaPasswordField("At least 6 characters", 24);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passwordField);
        card.add(inlineError("password"));
        card.add(Box.createVerticalStrut(12));

        // Confirm Password
        card.add(fieldLabel("Confirm Password  *"));
        card.add(Box.createVerticalStrut(5));
        confirmField = new HabeshaPasswordField("Re-enter your password", 24);
        confirmField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        confirmField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(confirmField);
        card.add(inlineError("confirm"));
        card.add(Box.createVerticalStrut(20));

        // ── Section label: Security PIN ───────────────────────────────────────
        card.add(sectionLabel("TRANSACTION PIN"));
        card.add(Box.createVerticalStrut(12));

        // PIN
        card.add(fieldLabel("4-Digit PIN  *  (used to authorise withdrawals)"));
        card.add(Box.createVerticalStrut(5));
        pinField = new HabeshaPasswordField("Enter 4 digits", 24);
        pinField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        pinField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(pinField);
        card.add(inlineError("pin"));
        card.add(Box.createVerticalStrut(12));

        // Confirm PIN
        card.add(fieldLabel("Confirm PIN  *"));
        card.add(Box.createVerticalStrut(5));
        confirmPinField = new HabeshaPasswordField("Re-enter your PIN", 24);
        confirmPinField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        confirmPinField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(confirmPinField);
        card.add(inlineError("confirmPin"));
        card.add(Box.createVerticalStrut(24));

        // ── Register button ───────────────────────────────────────────────────
        registerButton = new GoldButton("Create Account");
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerButton.addActionListener(e -> handleRegister());
        card.add(registerButton);

        card.add(Box.createVerticalStrut(16));

        // ── "Already have an account?" link ───────────────────────────────────
        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linkRow.setOpaque(false);
        linkRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        linkRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel already = new JLabel("Already have an account?  ");
        already.setFont(HabeshaTheme.FONT_SMALL);
        already.setForeground(HabeshaTheme.CREAM_DIM);
        linkRow.add(already);

        JLabel signInLink = new JLabel("Sign In");
        signInLink.setFont(HabeshaTheme.FONT_SMALL);
        signInLink.setForeground(HabeshaTheme.GOLD_PRIMARY);
        signInLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signInLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { goToLogin(); }
            @Override public void mouseEntered(MouseEvent e) {
                signInLink.setForeground(HabeshaTheme.GOLD_BRIGHT); }
            @Override public void mouseExited(MouseEvent e)  {
                signInLink.setForeground(HabeshaTheme.GOLD_PRIMARY); }
        });
        linkRow.add(signInLink);
        card.add(linkRow);

        // ── Enter key wiring ──────────────────────────────────────────────────
        fullNameField.addActionListener(e  -> emailField.requestFocusInWindow());
        emailField.addActionListener(e     -> phoneField.requestFocusInWindow());
        phoneField.addActionListener(e     -> usernameField.requestFocusInWindow());
        usernameField.addActionListener(e  -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e  -> confirmField.requestFocusInWindow());
        confirmField.addActionListener(e   -> pinField.requestFocusInWindow());
        pinField.addActionListener(e       -> confirmPinField.requestFocusInWindow());
        confirmPinField.addActionListener(e -> handleRegister());

        return card;
    }

    // ── Registration logic ────────────────────────────────────────────────────

    private void handleRegister() {
        clearAllErrors();

        boolean valid = true;

        String fullName   = fullNameField.getText().trim();
        String email      = emailField.getText().trim();
        String phone      = phoneField.getText().trim();
        String username   = usernameField.getText().trim();
        String password   = new String(passwordField.getPassword());
        String confirm    = new String(confirmField.getPassword());
        String pin        = new String(pinField.getPassword());
        String confirmPin = new String(confirmPinField.getPassword());

        if (fullName.isEmpty() || fullName.length() < 2) {
            showFieldError("fullName", "Full name must be at least 2 characters.");
            valid = false;
        }
        if (email.isEmpty()) {
            showFieldError("email", "Email address is required.");
            valid = false;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            showFieldError("email", "Enter a valid email address (e.g. you@example.com).");
            valid = false;
        }
        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            showFieldError("phone", "Enter a valid phone number (e.g. +251911000001).");
            valid = false;
        }
        if (username.isEmpty()) {
            showFieldError("username", "Username is required.");
            valid = false;
        } else if (!USERNAME_PATTERN.matcher(username).matches()) {
            showFieldError("username", "4–30 characters: letters, digits, dots or underscores only.");
            valid = false;
        }
        if (password.isEmpty()) {
            showFieldError("password", "Password is required.");
            valid = false;
        } else if (password.length() < 6) {
            showFieldError("password", "Password must be at least 6 characters.");
            valid = false;
        }
        if (confirm.isEmpty()) {
            showFieldError("confirm", "Please confirm your password.");
            valid = false;
        } else if (!password.equals(confirm)) {
            showFieldError("confirm", "Passwords do not match.");
            valid = false;
        }
        if (pin.isEmpty()) {
            showFieldError("pin", "PIN is required.");
            valid = false;
        } else if (!pin.matches("\\d{4}")) {
            showFieldError("pin", "PIN must be exactly 4 digits (numbers only).");
            valid = false;
        }
        if (confirmPin.isEmpty()) {
            showFieldError("confirmPin", "Please confirm your PIN.");
            valid = false;
        } else if (!pin.equals(confirmPin)) {
            showFieldError("confirmPin", "PINs do not match.");
            valid = false;
        }

        if (!valid) return;

        registerButton.setEnabled(false);
        registerButton.setText("Creating account…");

        final String finalUsername   = username;
        final String finalPassword   = password;
        final String finalPin        = pin;
        final String finalFullName   = fullName;
        final String finalEmail      = email;
        final String finalPhone      = phone.isEmpty() ? null : phone;

        Timer timer = new Timer(400, e -> {
            try {
                // Phase 6: pass PIN to register()
                String accountNumber = AuthService.getInstance().register(
                        finalUsername, finalPassword, finalPin,
                        finalFullName, finalEmail, finalPhone);

                // Wipe sensitive strings
                java.util.Arrays.fill(passwordField.getPassword(),   '\0');
                java.util.Arrays.fill(confirmField.getPassword(),    '\0');
                java.util.Arrays.fill(pinField.getPassword(),        '\0');
                java.util.Arrays.fill(confirmPinField.getPassword(), '\0');

                showSuccessDialog(accountNumber, finalFullName);

            } catch (ValidationException vex) {
                String field = vex.getField();
                if (field != null && errorLabels.containsKey(field)) {
                    showFieldError(field, vex.getMessage());
                } else {
                    showFieldError("username", vex.getMessage());
                }
                registerButton.setEnabled(true);
                registerButton.setText("Create Account");
            } catch (Exception ex) {
                showFieldError("username", "Registration failed: " + ex.getMessage());
                registerButton.setEnabled(true);
                registerButton.setText("Create Account");
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    // ── Success dialog ────────────────────────────────────────────────────────

    private void showSuccessDialog(String accountNumber, String fullName) {
        // Custom dialog styled to match the app aesthetic
        JDialog dialog = new JDialog(this, "Account Created", true);
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(HabeshaTheme.BLACK_CARD);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));

        // Gold checkmark
        JLabel checkmark = new JLabel("✓", SwingConstants.CENTER);
        checkmark.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 44));
        checkmark.setForeground(HabeshaTheme.GOLD_PRIMARY);
        checkmark.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(checkmark);
        panel.add(Box.createVerticalStrut(12));

        JLabel heading = new JLabel("Account Created!", SwingConstants.CENTER);
        heading.setFont(HabeshaTheme.FONT_TITLE);
        heading.setForeground(HabeshaTheme.CREAM_LIGHT);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(6));

        JLabel nameLabel = new JLabel("Welcome, " + fullName, SwingConstants.CENTER);
        nameLabel.setFont(HabeshaTheme.FONT_BODY);
        nameLabel.setForeground(HabeshaTheme.CREAM_DIM);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(nameLabel);
        panel.add(Box.createVerticalStrut(20));

        // Account number — the most important piece of info
        JLabel acctHint = new JLabel("Your Account Number", SwingConstants.CENTER);
        acctHint.setFont(HabeshaTheme.FONT_SMALL);
        acctHint.setForeground(HabeshaTheme.CREAM_DIM);
        acctHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(acctHint);
        panel.add(Box.createVerticalStrut(6));

        JLabel acctNumber = new JLabel(accountNumber, SwingConstants.CENTER);
        acctNumber.setFont(HabeshaTheme.FONT_AMOUNT);
        acctNumber.setForeground(HabeshaTheme.GOLD_PRIMARY);
        acctNumber.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(acctNumber);
        panel.add(Box.createVerticalStrut(6));

        JLabel saveHint = new JLabel("Save this — you will need it to log in.", SwingConstants.CENTER);
        saveHint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        saveHint.setForeground(HabeshaTheme.CREAM_DIM);
        saveHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(saveHint);
        panel.add(Box.createVerticalStrut(24));

        GoldButton okBtn = new GoldButton("Go to Sign In");
        okBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        okBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        okBtn.addActionListener(e -> {
            dialog.dispose();
            goToLogin();
        });
        panel.add(okBtn);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /** Closes this frame and opens (or re-focuses) LoginFrame. */
    private void goToLogin() {
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    /**
     * Creates and registers an inline error label for {@code fieldKey}.
     * The label is hidden by default; {@link #showFieldError} makes it visible.
     */
    private JLabel inlineError(String fieldKey) {
        JLabel lbl = new JLabel(" ");   // non-empty to hold layout height
        lbl.setFont(HabeshaTheme.FONT_SMALL);
        lbl.setForeground(HabeshaTheme.RED_DANGER);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        errorLabels.put(fieldKey, lbl);
        return lbl;
    }

    private void showFieldError(String fieldKey, String message) {
        JLabel lbl = errorLabels.get(fieldKey);
        if (lbl != null) {
            lbl.setText("⚠  " + message);
            lbl.repaint();
        }
    }

    private void clearAllErrors() {
        errorLabels.values().forEach(lbl -> lbl.setText(" "));
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(HabeshaTheme.FONT_SMALL);
        lbl.setForeground(HabeshaTheme.CREAM_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lbl.setForeground(HabeshaTheme.GOLD_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }
}
