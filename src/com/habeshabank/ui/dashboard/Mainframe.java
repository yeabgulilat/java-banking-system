package com.habeshabank.ui.dashboard;

import com.habeshabank.model.UserSession;
import com.habeshabank.service.AuthService;
import com.habeshabank.service.SessionTimeoutManager;
import com.habeshabank.ui.Refreshable;
import com.habeshabank.ui.components.*;
import com.habeshabank.ui.deposit.DepositPanel;
import com.habeshabank.ui.equb.EqubPanel;
import com.habeshabank.ui.history.TransactionHistoryPanel;
import com.habeshabank.ui.iddir.IddirPanel;
import com.habeshabank.ui.settings.SettingsPanel;
import com.habeshabank.ui.theme.HabeshaTheme;
import com.habeshabank.ui.transfer.TransferPanel;
import com.habeshabank.ui.withdraw.WithdrawPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application shell — hosts sidebar navigation and CardLayout content area.
 *
 * Phase 3 additions
 * ─────────────────
 * • Holds a Map<pageKey, Component> of all content panels.
 * • {@link #refreshAllUI()} iterates every registered panel that implements
 *   {@link Refreshable} and calls refreshData() on it.
 * • {@link #navigateTo(String)} now also calls refreshData() on the panel
 *   being shown, so navigating to any screen always shows current data.
 * • All existing layout, styling, sidebar structure, and logout flow unchanged.
 */
public class MainFrame extends JFrame {

    // Navigation identifiers (unchanged)
    public static final String PAGE_DASHBOARD = "dashboard";
    public static final String PAGE_DEPOSIT   = "deposit";
    public static final String PAGE_WITHDRAW  = "withdraw";
    public static final String PAGE_TRANSFER  = "transfer";
    public static final String PAGE_HISTORY   = "history";
    public static final String PAGE_EQUB      = "equb";
    public static final String PAGE_IDDIR     = "iddir";
    public static final String PAGE_SETTINGS  = "settings";   // Phase 6

    private final List<NavButton>         navButtons   = new ArrayList<>();
    private final Map<NavButton, String>  navButtonPageKeys = new HashMap<>();
    private final CardLayout              cardLayout   = new CardLayout();
    private final JPanel                  contentArea  = new JPanel(cardLayout);
    private final Map<String, Component>  panelsByKey  = new HashMap<>();
    private String                        activePage   = PAGE_DASHBOARD;

    // Phase 6: session timeout
    private final SessionTimeoutManager   sessionTimeout =
            new SessionTimeoutManager(this::performLogout);

    public MainFrame() {
        setTitle("Habesha Digital Banking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null);

        initComponents();
        navigateTo(PAGE_DASHBOARD);

        // Phase 6: start inactivity timer after UI is ready
        sessionTimeout.start();
    }

    // ── Central Refresh ───────────────────────────────────────────────────────

    /**
     * Refreshes ALL content panels that implement {@link Refreshable}.
     *
     * Call this after every successful transaction (deposit, withdraw, transfer,
     * equb, iddir) so the entire UI — including the dashboard and history table —
     * reflects the new state immediately, regardless of which panel the user
     * is currently viewing.
     *
     * Must be called on the EDT; all callers (Swing action listeners) satisfy this.
     */
    public void refreshAllUI() {
        panelsByKey.values().forEach(panel -> {
            if (panel instanceof Refreshable r) {
                r.refreshData();
            }
        });
    }

    // ── Components ────────────────────────────────────────────────────────────

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(HabeshaTheme.BLACK_DEEP);

        root.add(buildTopBar(),  BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContent(), BorderLayout.CENTER);

        setContentPane(root);
    }

    // ── Top Bar (unchanged) ───────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(HabeshaTheme.BLACK_RICH);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(HabeshaTheme.BLACK_BORDER);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 56));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel logoMark = new JLabel("✦");
        logoMark.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 20));
        logoMark.setForeground(HabeshaTheme.GOLD_PRIMARY);
        left.add(logoMark);
        JLabel bankName = new JLabel("HABESHA BANK");
        bankName.setFont(HabeshaTheme.FONT_HEADING);
        bankName.setForeground(HabeshaTheme.CREAM_LIGHT);
        left.add(bankName);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        UserSession session = UserSession.getInstance();
        JLabel acctLabel = new JLabel(session.getAccountNumber());
        acctLabel.setFont(HabeshaTheme.FONT_MONO);
        acctLabel.setForeground(HabeshaTheme.GOLD_MUTED);
        right.add(acctLabel);
        JLabel userLabel = new JLabel(session.getFullName());
        userLabel.setFont(HabeshaTheme.FONT_BODY_BOLD);
        userLabel.setForeground(HabeshaTheme.CREAM_LIGHT);
        right.add(userLabel);
        GoldButton logoutBtn = new GoldButton("Logout", GoldButton.Style.OUTLINE);
        logoutBtn.setPreferredSize(new Dimension(90, 32));
        logoutBtn.addActionListener(e -> confirmLogout());
        right.add(logoutBtn);

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Sidebar (unchanged) ───────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(HabeshaTheme.BLACK_RICH);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(HabeshaTheme.BLACK_BORDER);
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setOpaque(false);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(230, Integer.MAX_VALUE));
        sidebar.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        EthiopianPatternPanel sidePattern =
                new EthiopianPatternPanel(EthiopianPatternPanel.Orientation.HORIZONTAL, 20);
        sidePattern.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        sidebar.add(sidePattern);
        sidebar.add(Box.createVerticalStrut(16));

        sidebar.add(sectionHeader("MAIN"));
        sidebar.add(makeNavButton("⌂", "Dashboard",   PAGE_DASHBOARD));
        sidebar.add(makeNavButton("↓", "Deposit",      PAGE_DEPOSIT));
        sidebar.add(makeNavButton("↑", "Withdraw",     PAGE_WITHDRAW));
        sidebar.add(makeNavButton("⇌", "Transfer",     PAGE_TRANSFER));
        sidebar.add(makeNavButton("≡", "Transactions", PAGE_HISTORY));

        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(makeSeparator());
        sidebar.add(Box.createVerticalStrut(12));

        sidebar.add(sectionHeader("COMMUNITY"));
        sidebar.add(makeNavButton("◎", "Digital Equb",  PAGE_EQUB));
        sidebar.add(makeNavButton("♦", "Iddir Support", PAGE_IDDIR));

        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(makeSeparator());
        sidebar.add(Box.createVerticalStrut(12));

        sidebar.add(sectionHeader("ACCOUNT"));
        sidebar.add(makeNavButton("⚙", "Settings",      PAGE_SETTINGS));

        sidebar.add(Box.createVerticalGlue());

        JLabel version = new JLabel("  v1.0.0  •  Beta");
        version.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        version.setForeground(HabeshaTheme.BLACK_BORDER);
        sidebar.add(version);

        return sidebar;
    }

    private NavButton makeNavButton(String icon, String label, String pageKey) {
        NavButton btn = new NavButton(icon, label);
        btn.setOnClick(() -> navigateTo(pageKey));
        navButtons.add(btn);
        navButtonPageKeys.put(btn, pageKey);
        return btn;
    }

    private JLabel sectionHeader(String text) {
        JLabel lbl = new JLabel("  " + text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lbl.setForeground(HabeshaTheme.GOLD_MUTED);
        lbl.setPreferredSize(new Dimension(Integer.MAX_VALUE, 24));
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return lbl;
    }

    private JSeparator makeSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(HabeshaTheme.BLACK_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    // ── Content Area ──────────────────────────────────────────────────────────

    private JPanel buildContent() {
        contentArea.setBackground(HabeshaTheme.BLACK_DEEP);

        registerPanel(PAGE_DASHBOARD, new DashboardPanel(this));
        registerPanel(PAGE_DEPOSIT,   new DepositPanel(this));
        registerPanel(PAGE_WITHDRAW,  new WithdrawPanel(this));
        registerPanel(PAGE_TRANSFER,  new TransferPanel(this));
        registerPanel(PAGE_HISTORY,   new TransactionHistoryPanel(this));
        registerPanel(PAGE_EQUB,      new EqubPanel(this));
        registerPanel(PAGE_IDDIR,     new IddirPanel(this));
        registerPanel(PAGE_SETTINGS,  new SettingsPanel(this));   // Phase 6

        return contentArea;
    }

    /** Adds a panel to both the CardLayout and the refreshable registry. */
    private void registerPanel(String key, Component panel) {
        contentArea.add(panel, key);
        panelsByKey.put(key, panel);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /**
     * Shows the named page and refreshes it with current data.
     * Nav-button active states updated as before.
     */
    public void navigateTo(String pageKey) {
        activePage = pageKey;
        cardLayout.show(contentArea, pageKey);
        sessionTimeout.resetTimer();

        // Refresh the panel being shown so it always has current data
        Component panel = panelsByKey.get(pageKey);
        if (panel instanceof Refreshable r) {
            r.refreshData();
        }

        // Update nav button active states (unchanged logic)
        navButtons.forEach(btn -> btn.setActive(false));
        navButtons.stream()
                .filter(btn -> matchesPage(btn, pageKey))
                .findFirst()
                .ifPresent(btn -> btn.setActive(true));
    }

    private boolean matchesPage(NavButton btn, String pageKey) {
        return pageKey.equals(navButtonPageKeys.get(btn));
    }

    // ── Session Timeout ───────────────────────────────────────────────────────

    /**
     * Resets the inactivity timer. Call after every successful transaction
     * so a long-running operation doesn't trigger a timeout mid-session.
     */
    public void resetSessionTimeout() {
        sessionTimeout.resetTimer();
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    private void confirmLogout() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to sign out?",
                "Sign Out", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            performLogout();
        }
    }

    /**
     * Clears the session, stops the timeout timer, closes this frame,
     * and opens LoginFrame. Called by both the Logout button and the
     * session timeout callback.
     */
    private void performLogout() {
        sessionTimeout.stop();
        AuthService.getInstance().logout();
        dispose();
        SwingUtilities.invokeLater(() -> {
            com.habeshabank.ui.login.LoginFrame login =
                    new com.habeshabank.ui.login.LoginFrame();
            login.setVisible(true);
        });
    }
}
