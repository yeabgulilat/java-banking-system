package com.habeshabank.ui.dashboard;

import com.habeshabank.model.UserSession;
import com.habeshabank.ui.components.*;
import com.habeshabank.ui.deposit.DepositPanel;
import com.habeshabank.ui.equb.EqubPanel;
import com.habeshabank.ui.history.TransactionHistoryPanel;
import com.habeshabank.ui.iddir.IddirPanel;
import com.habeshabank.ui.theme.HabeshaTheme;
import com.habeshabank.ui.transfer.TransferPanel;
import com.habeshabank.ui.withdraw.WithdrawPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The main application shell after login.
 * Hosts the sidebar navigation and a CardLayout content area.
 */
public class MainFrame extends JFrame {

    // Navigation identifiers
    public static final String PAGE_DASHBOARD = "dashboard";
    public static final String PAGE_DEPOSIT   = "deposit";
    public static final String PAGE_WITHDRAW  = "withdraw";
    public static final String PAGE_TRANSFER  = "transfer";
    public static final String PAGE_HISTORY   = "history";
    public static final String PAGE_EQUB      = "equb";
    public static final String PAGE_IDDIR     = "iddir";

    private final List<NavButton> navButtons  = new ArrayList<>();
    private final CardLayout      cardLayout  = new CardLayout();
    private final JPanel          contentArea = new JPanel(cardLayout);
    private String                activePage  = PAGE_DASHBOARD;

    public MainFrame() {
        setTitle("Habesha Digital Banking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null);

        initComponents();
        navigateTo(PAGE_DASHBOARD);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(HabeshaTheme.BLACK_DEEP);

        root.add(buildTopBar(),   BorderLayout.NORTH);
        root.add(buildSidebar(),  BorderLayout.WEST);
        root.add(buildContent(),  BorderLayout.CENTER);

        setContentPane(root);
    }

    // ── Top Bar ───────────────────────────────────────────────────────────────

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

        // Left: logo area
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

        // Right: user info + logout
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

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(HabeshaTheme.BLACK_RICH);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Right border
                g2.setColor(HabeshaTheme.BLACK_BORDER);
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setOpaque(false);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(230, Integer.MAX_VALUE));
        sidebar.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        // Pattern accent strip at top of sidebar
        EthiopianPatternPanel sidePattern =
                new EthiopianPatternPanel(EthiopianPatternPanel.Orientation.HORIZONTAL, 20);
        sidePattern.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        sidebar.add(sidePattern);
        sidebar.add(Box.createVerticalStrut(16));

        // Section: Main
        sidebar.add(sectionHeader("MAIN"));
        sidebar.add(makeNavButton("⌂", "Dashboard",    PAGE_DASHBOARD));
        sidebar.add(makeNavButton("↓", "Deposit",       PAGE_DEPOSIT));
        sidebar.add(makeNavButton("↑", "Withdraw",      PAGE_WITHDRAW));
        sidebar.add(makeNavButton("⇌", "Transfer",      PAGE_TRANSFER));
        sidebar.add(makeNavButton("≡", "Transactions",  PAGE_HISTORY));

        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(makeSeparator());
        sidebar.add(Box.createVerticalStrut(12));

        // Section: Community
        sidebar.add(sectionHeader("COMMUNITY"));
        sidebar.add(makeNavButton("◎", "Digital Equb",  PAGE_EQUB));
        sidebar.add(makeNavButton("♦", "Iddir Support", PAGE_IDDIR));

        sidebar.add(Box.createVerticalGlue());

        // Bottom: version info
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

        contentArea.add(new DashboardPanel(this), PAGE_DASHBOARD);
        contentArea.add(new DepositPanel(this),            PAGE_DEPOSIT);
        contentArea.add(new WithdrawPanel(this),           PAGE_WITHDRAW);
        contentArea.add(new TransferPanel(this),           PAGE_TRANSFER);
        contentArea.add(new TransactionHistoryPanel(this), PAGE_HISTORY);
        contentArea.add(new EqubPanel(this),               PAGE_EQUB);
        contentArea.add(new IddirPanel(this),              PAGE_IDDIR);

        return contentArea;
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public void navigateTo(String pageKey) {
        activePage = pageKey;
        cardLayout.show(contentArea, pageKey);

        // Update nav button active states
        navButtons.forEach(btn -> btn.setActive(false));
        navButtons.stream()
                .filter(btn -> btn.isActive() || matchesPage(btn, pageKey))
                .findFirst()
                .ifPresent(btn -> btn.setActive(true));
    }

    /** Simple heuristic to match a NavButton to a page key by label. */
    private boolean matchesPage(NavButton btn, String pageKey) {
        // Use the order they were added — rely on navButtons list index
        String[] pages = { PAGE_DASHBOARD, PAGE_DEPOSIT, PAGE_WITHDRAW,
                PAGE_TRANSFER, PAGE_HISTORY, PAGE_EQUB, PAGE_IDDIR };
        int idx = navButtons.indexOf(btn);
        return idx >= 0 && idx < pages.length && pages[idx].equals(pageKey);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    private void confirmLogout() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to sign out?",
                "Sign Out", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            UserSession.clearSession();
            dispose();
            SwingUtilities.invokeLater(() -> {
                com.habeshabank.ui.login.LoginFrame login = new com.habeshabank.ui.login.LoginFrame();
                login.setVisible(true);
            });
        }
    }
}