package com.habeshabank.ui.dashboard;

import com.habeshabank.model.Transaction;
import com.habeshabank.model.UserSession;
import com.habeshabank.ui.components.*;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Home dashboard showing account summary, stat cards, and recent transactions.
 */
public class DashboardPanel extends JPanel {

    private final MainFrame mainFrame;
    private StatCard balanceCard;
    private StatCard depositCard;
    private StatCard withdrawCard;
    private StatCard equbCard;
    private DefaultTableModel recentTableModel;

    public DashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(true);
        setBackground(HabeshaTheme.BLACK_DEEP);
        setLayout(new BorderLayout());

        add(buildScrollableContent(), BorderLayout.CENTER);
        loadDemoData();
    }

    private JScrollPane buildScrollableContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        content.add(buildPageHeader());
        content.add(Box.createVerticalStrut(24));
        content.add(buildStatCards());
        content.add(Box.createVerticalStrut(28));
        content.add(buildQuickActions());
        content.add(Box.createVerticalStrut(28));
        content.add(buildRecentTransactions());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ── Page Header ───────────────────────────────────────────────────────────

    private JPanel buildPageHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        UserSession session = UserSession.getInstance();

        JLabel greeting = new JLabel("Good " + timeOfDay() + ", " + firstName(session.getFullName()) + " 👋");
        greeting.setFont(HabeshaTheme.FONT_DISPLAY);
        greeting.setForeground(HabeshaTheme.CREAM_LIGHT);

        JLabel dateLabel = new JLabel(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
        dateLabel.setFont(HabeshaTheme.FONT_SMALL);
        dateLabel.setForeground(HabeshaTheme.CREAM_DIM);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(greeting);
        left.add(Box.createVerticalStrut(4));
        left.add(dateLabel);

        // Account badge
        JPanel badge = buildAccountBadge(session);

        panel.add(left,  BorderLayout.WEST);
        panel.add(badge, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildAccountBadge(UserSession session) {
        JPanel badge = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(HabeshaTheme.BLACK_CARD);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.setColor(HabeshaTheme.GOLD_MUTED);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setLayout(new BoxLayout(badge, BoxLayout.Y_AXIS));
        badge.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel typeLabel = new JLabel(session.getAccountType());
        typeLabel.setFont(HabeshaTheme.FONT_SMALL);
        typeLabel.setForeground(HabeshaTheme.GOLD_PRIMARY);
        typeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel acctLabel = new JLabel(session.getAccountNumber());
        acctLabel.setFont(HabeshaTheme.FONT_MONO);
        acctLabel.setForeground(HabeshaTheme.CREAM_MID);
        acctLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        badge.add(typeLabel);
        badge.add(Box.createVerticalStrut(4));
        badge.add(acctLabel);
        return badge;
    }

    // ── Stat Cards ────────────────────────────────────────────────────────────

    private JPanel buildStatCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);

        balanceCard  = new StatCard("Current Balance",    "—",     "Available funds",    HabeshaTheme.GOLD_PRIMARY);
        depositCard  = new StatCard("Total Deposits",     "—",     "This month",          HabeshaTheme.GREEN_SUCCESS);
        withdrawCard = new StatCard("Total Withdrawals",  "—",     "This month",          HabeshaTheme.RED_DANGER);
        equbCard     = new StatCard("Equb Contributions", "—",     "Active rounds",       HabeshaTheme.BLUE_INFO);

        grid.add(balanceCard);
        grid.add(depositCard);
        grid.add(withdrawCard);
        grid.add(equbCard);

        return grid;
    }

    // ── Quick Actions ─────────────────────────────────────────────────────────

    private JPanel buildQuickActions() {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Quick Actions");
        title.setFont(HabeshaTheme.FONT_HEADING);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(title);
        section.add(Box.createVerticalStrut(14));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttonRow.setOpaque(false);

        String[][] actions = {
                { "↓  Deposit",       MainFrame.PAGE_DEPOSIT   },
                { "↑  Withdraw",      MainFrame.PAGE_WITHDRAW  },
                { "⇌  Transfer",      MainFrame.PAGE_TRANSFER  },
                { "◎  Equb",          MainFrame.PAGE_EQUB      },
                { "♦  Iddir",         MainFrame.PAGE_IDDIR     },
                { "≡  History",       MainFrame.PAGE_HISTORY   },
        };

        for (String[] action : actions) {
            GoldButton btn = new GoldButton(action[0],
                    action[0].contains("Withdraw") || action[0].contains("Iddir")
                            ? GoldButton.Style.OUTLINE : GoldButton.Style.PRIMARY);
            btn.setPreferredSize(new Dimension(130, 38));
            String page = action[1];
            btn.addActionListener(e -> mainFrame.navigateTo(page));
            buttonRow.add(btn);
        }

        section.add(buttonRow);
        return section;
    }

    // ── Recent Transactions ───────────────────────────────────────────────────

    private JPanel buildRecentTransactions() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);

        // Header row
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("Recent Transactions");
        title.setFont(HabeshaTheme.FONT_HEADING);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);
        header.add(title, BorderLayout.WEST);

        GoldButton viewAll = new GoldButton("View All", GoldButton.Style.OUTLINE);
        viewAll.setPreferredSize(new Dimension(90, 32));
        viewAll.addActionListener(e -> mainFrame.navigateTo(MainFrame.PAGE_HISTORY));
        header.add(viewAll, BorderLayout.EAST);
        section.add(header, BorderLayout.NORTH);

        // Table
        String[] cols = { "Date", "Description", "Type", "Amount", "Balance" };
        recentTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(recentTableModel);
        table.setRowHeight(40);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 4));
        table.setBackground(HabeshaTheme.BLACK_CARD);
        table.setForeground(HabeshaTheme.CREAM_MID);
        table.setFont(HabeshaTheme.FONT_BODY);
        table.getTableHeader().setFont(HabeshaTheme.FONT_BODY_BOLD);
        table.getTableHeader().setBackground(HabeshaTheme.BLACK_PANEL);
        table.getTableHeader().setForeground(HabeshaTheme.GOLD_PRIMARY);
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());

        // Amount column coloring
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String text = val != null ? val.toString() : "";
                lbl.setForeground(text.startsWith("+") ? HabeshaTheme.GREEN_SUCCESS : HabeshaTheme.RED_DANGER);
                lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                return lbl;
            }
        });

        // Balance column right-align
        DefaultTableCellRenderer rightAlign = new DefaultTableCellRenderer();
        rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(rightAlign);

        // Column widths
        int[] widths = { 140, 260, 120, 120, 130 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(HabeshaTheme.BLACK_BORDER));
        scroll.setBackground(HabeshaTheme.BLACK_CARD);
        scroll.getViewport().setBackground(HabeshaTheme.BLACK_CARD);

        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    // ── Demo Data ─────────────────────────────────────────────────────────────

    private void loadDemoData() {
        UserSession session = UserSession.getInstance();

        balanceCard.setValue(String.format("%.2f ETB", session.getBalance()));
        balanceCard.setSubtitle("Last updated: today");
        depositCard.setValue("12,400.00");
        withdrawCard.setValue("3,200.00");
        equbCard.setValue("2 Active");

        // Demo recent transactions
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");
        Object[][] rows = {
                { LocalDateTime.now().minusHours(2).format(fmt), "Deposit – Cash", "DEPOSIT",
                        "+5,000.00 ETB",  "47,850.00 ETB" },
                { LocalDateTime.now().minusDays(1).format(fmt),  "Transfer to Kaleb Girma", "TRANSFER",
                        "-2,500.00 ETB",  "42,850.00 ETB" },
                { LocalDateTime.now().minusDays(2).format(fmt),  "Equb Round #7 Contribution", "EQUB",
                        "-500.00 ETB",    "45,350.00 ETB" },
                { LocalDateTime.now().minusDays(3).format(fmt),  "ATM Withdrawal – Bole", "WITHDRAWAL",
                        "-700.00 ETB",    "45,850.00 ETB" },
                { LocalDateTime.now().minusDays(5).format(fmt),  "Salary Credit", "DEPOSIT",
                        "+10,000.00 ETB", "46,550.00 ETB" },
        };
        for (Object[] row : rows) recentTableModel.addRow(row);
    }

    private String timeOfDay() {
        int hour = LocalDateTime.now().getHour();
        if (hour < 12) return "Morning";
        if (hour < 17) return "Afternoon";
        return "Evening";
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "User";
        return fullName.split(" ")[0];
    }
}