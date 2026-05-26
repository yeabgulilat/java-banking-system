package com.habeshabank.ui.settings;

import com.habeshabank.exception.AuthenticationException;
import com.habeshabank.exception.ValidationException;
import com.habeshabank.model.UserSession;
import com.habeshabank.service.AuthService;
import com.habeshabank.service.SessionTimeoutManager;
import com.habeshabank.ui.Refreshable;
import com.habeshabank.ui.components.*;
import com.habeshabank.ui.dashboard.MainFrame;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Settings panel — Phase 6.
 *
 * Sections
 * ────────
 * 1. Account Overview   — read-only: account number, type, email, session info
 * 2. Change PIN         — old PIN + new PIN + confirm, wired to AuthService.changePin()
 * 3. Session Security   — shows timeout duration, "Logout Now" shortcut
 *
 * Design rules
 * ────────────
 * • Same BLACK_DEEP background, SectionPanel cards, GOLD_PRIMARY headings.
 * • Inline error labels under each PIN field — same pattern as RegisterFrame.
 * • No popup dialogs for validation failures; success shown as a brief
 *   green status label that auto-clears after 3 seconds.
 * • Implements Refreshable so account info refreshes on navigation.
 */
public class SettingsPanel extends JPanel implements Refreshable {

    private final MainFrame mainFrame;

    // ── Account info labels (refreshed by refreshData) ────────────────────────
    private JLabel accountNumberLabel;
    private JLabel accountTypeLabel;
    private JLabel emailLabel;
    private JLabel balanceLabel;

    // ── Change PIN fields ─────────────────────────────────────────────────────
    private HabeshaPasswordField currentPinField;
    private HabeshaPasswordField newPinField;
    private HabeshaPasswordField confirmNewPinField;
    private JLabel               pinStatusLabel;

    public SettingsPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(true);
        setBackground(HabeshaTheme.BLACK_DEEP);
        setLayout(new BorderLayout());
        add(buildScrollableContent(), BorderLayout.CENTER);
    }

    // ── Refreshable ───────────────────────────────────────────────────────────

    @Override
    public void refreshData() {
        UserSession session = UserSession.getInstance();
        if (accountNumberLabel != null) {
            accountNumberLabel.setText(session.getAccountNumber() != null
                    ? session.getAccountNumber() : "—");
        }
        if (accountTypeLabel != null) {
            accountTypeLabel.setText(session.getAccountType() != null
                    ? session.getAccountType() : "—");
        }
        if (emailLabel != null) {
            emailLabel.setText(session.getEmail() != null
                    ? session.getEmail() : "—");
        }
        if (balanceLabel != null) {
            balanceLabel.setText(String.format("%,.2f ETB", session.getBalance()));
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private JScrollPane buildScrollableContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        content.add(buildPageHeader());
        content.add(Box.createVerticalStrut(28));

        // Two-column layout: account info on left, security on right
        JPanel columns = new JPanel(new GridLayout(1, 2, 24, 0));
        columns.setOpaque(false);
        columns.add(buildAccountInfoCard());
        columns.add(buildChangePinCard());
        content.add(columns);

        content.add(Box.createVerticalStrut(24));
        content.add(buildSessionSecurityCard());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel buildPageHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Settings  ⚙");
        title.setFont(HabeshaTheme.FONT_DISPLAY);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);

        JLabel sub = new JLabel("Manage your account security and preferences");
        sub.setFont(HabeshaTheme.FONT_SUBHEAD);
        sub.setForeground(HabeshaTheme.CREAM_DIM);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        panel.add(left, BorderLayout.WEST);
        return panel;
    }

    // ── Account Info Card ─────────────────────────────────────────────────────

    private JPanel buildAccountInfoCard() {
        JPanel card = new SectionPanel("", null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel heading = new JLabel("Account Overview");
        heading.setFont(HabeshaTheme.FONT_HEADING);
        heading.setForeground(HabeshaTheme.GOLD_PRIMARY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(20));

        UserSession session = UserSession.getInstance();

        accountNumberLabel = infoRow(card, "Account Number",
                session.getAccountNumber() != null ? session.getAccountNumber() : "—");
        accountTypeLabel   = infoRow(card, "Account Type",
                session.getAccountType()   != null ? session.getAccountType()   : "—");
        emailLabel         = infoRow(card, "Email",
                session.getEmail()         != null ? session.getEmail()         : "—");
        balanceLabel       = infoRow(card, "Current Balance",
                String.format("%,.2f ETB", session.getBalance()));

        card.add(Box.createVerticalStrut(20));

        // Read-only badge
        JLabel badge = new JLabel("  Read-only — contact support to update personal details");
        badge.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        badge.setForeground(HabeshaTheme.CREAM_DIM);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(badge);

        return card;
    }

    /** Adds a label-value row to a card and returns the value JLabel for later updates. */
    private JLabel infoRow(JPanel card, String labelText, String valueText) {
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(HabeshaTheme.FONT_SMALL);
        lbl.setForeground(HabeshaTheme.CREAM_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lbl);
        card.add(Box.createVerticalStrut(3));

        JLabel val = new JLabel(valueText);
        val.setFont(HabeshaTheme.FONT_BODY_BOLD);
        val.setForeground(HabeshaTheme.CREAM_LIGHT);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(val);
        card.add(Box.createVerticalStrut(14));

        return val;
    }

    // ── Change PIN Card ───────────────────────────────────────────────────────

    private JPanel buildChangePinCard() {
        JPanel card = new SectionPanel("", null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel heading = new JLabel("Change Transaction PIN");
        heading.setFont(HabeshaTheme.FONT_HEADING);
        heading.setForeground(HabeshaTheme.GOLD_PRIMARY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(6));

        JLabel hint = new JLabel("Your PIN is required every time you make a withdrawal.");
        hint.setFont(HabeshaTheme.FONT_SMALL);
        hint.setForeground(HabeshaTheme.CREAM_DIM);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hint);
        card.add(Box.createVerticalStrut(20));

        // Current PIN
        card.add(pinFieldLabel("Current PIN"));
        card.add(Box.createVerticalStrut(5));
        currentPinField = new HabeshaPasswordField("Enter current PIN", 16);
        currentPinField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        currentPinField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(currentPinField);
        card.add(Box.createVerticalStrut(14));

        // New PIN
        card.add(pinFieldLabel("New PIN  (4 digits)"));
        card.add(Box.createVerticalStrut(5));
        newPinField = new HabeshaPasswordField("Enter new 4-digit PIN", 16);
        newPinField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        newPinField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(newPinField);
        card.add(Box.createVerticalStrut(14));

        // Confirm New PIN
        card.add(pinFieldLabel("Confirm New PIN"));
        card.add(Box.createVerticalStrut(5));
        confirmNewPinField = new HabeshaPasswordField("Re-enter new PIN", 16);
        confirmNewPinField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        confirmNewPinField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(confirmNewPinField);
        card.add(Box.createVerticalStrut(16));

        // Status label — shown on success or error
        pinStatusLabel = new JLabel(" ");
        pinStatusLabel.setFont(HabeshaTheme.FONT_SMALL);
        pinStatusLabel.setForeground(HabeshaTheme.RED_DANGER);
        pinStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(pinStatusLabel);
        card.add(Box.createVerticalStrut(8));

        // Submit button
        GoldButton changePinBtn = new GoldButton("Update PIN");
        changePinBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        changePinBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        changePinBtn.addActionListener(e -> handleChangePin());
        card.add(changePinBtn);

        // Enter-key chain
        currentPinField.addActionListener(e  -> newPinField.requestFocusInWindow());
        newPinField.addActionListener(e      -> confirmNewPinField.requestFocusInWindow());
        confirmNewPinField.addActionListener(e -> handleChangePin());

        return card;
    }

    // ── Session Security Card ─────────────────────────────────────────────────

    private JPanel buildSessionSecurityCard() {
        JPanel card = new SectionPanel("", null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel heading = new JLabel("Session Security");
        heading.setFont(HabeshaTheme.FONT_HEADING);
        heading.setForeground(HabeshaTheme.GOLD_PRIMARY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(16));

        int timeoutMin = SessionTimeoutManager.TIMEOUT_MS / 60_000;

        String[] info = {
                "• Auto-logout after " + timeoutMin + " minutes of inactivity",
                "• PIN is required for every withdrawal",
                "• Account locks after 5 incorrect password attempts",
                "• Passwords and PINs are stored as secure hashes — never plain text",
                "• Session data is cleared immediately on logout"
        };

        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        for (String line : info) {
            JLabel lbl = new JLabel(line);
            lbl.setFont(HabeshaTheme.FONT_BODY);
            lbl.setForeground(HabeshaTheme.CREAM_MID);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoPanel.add(lbl);
            infoPanel.add(Box.createVerticalStrut(6));
        }

        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        card.add(infoPanel);
        card.add(Box.createVerticalStrut(16));

        JLabel sessionTimeLabel = new JLabel("Session started: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm  dd MMM yyyy")));
        sessionTimeLabel.setFont(HabeshaTheme.FONT_SMALL);
        sessionTimeLabel.setForeground(HabeshaTheme.CREAM_DIM);
        sessionTimeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sessionTimeLabel);

        return card;
    }

    // ── Change PIN logic ──────────────────────────────────────────────────────

    private void handleChangePin() {
        char[] current    = currentPinField.getPassword();
        char[] newPin     = newPinField.getPassword();
        char[] confirmPin = confirmNewPinField.getPassword();

        if (current.length == 0) {
            showPinError("Please enter your current PIN.");
            return;
        }
        if (newPin.length == 0) {
            showPinError("Please enter your new PIN.");
            return;
        }

        try {
            AuthService.getInstance().changePin(current, newPin, confirmPin);

            // Success
            currentPinField.setText("");
            newPinField.setText("");
            confirmNewPinField.setText("");

            pinStatusLabel.setForeground(new Color(0x2E, 0x7D, 0x4F)); // GREEN_SUCCESS
            pinStatusLabel.setText("✓  PIN updated successfully.");
            mainFrame.resetSessionTimeout();

            // Auto-clear success message after 3 seconds
            Timer clearTimer = new Timer(3000, e -> {
                pinStatusLabel.setText(" ");
                pinStatusLabel.setForeground(HabeshaTheme.RED_DANGER);
            });
            clearTimer.setRepeats(false);
            clearTimer.start();

        } catch (AuthenticationException | ValidationException ex) {
            showPinError(ex.getMessage());
            // Clear only the PIN input fields; current PIN stays for retry
            newPinField.setText("");
            confirmNewPinField.setText("");
        }
    }

    private void showPinError(String msg) {
        pinStatusLabel.setForeground(HabeshaTheme.RED_DANGER);
        pinStatusLabel.setText("⚠  " + msg);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private JLabel pinFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(HabeshaTheme.FONT_SMALL);
        lbl.setForeground(HabeshaTheme.CREAM_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }
}
