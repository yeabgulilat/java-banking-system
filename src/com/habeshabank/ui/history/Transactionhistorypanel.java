package com.habeshabank.ui.history;

import com.habeshabank.ui.components.*;
import com.habeshabank.ui.dashboard.MainFrame;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Transaction history panel with search/filter capabilities.
 * Data is demo — database wiring comes later.
 */
public class TransactionHistoryPanel extends JPanel {

    private final MainFrame mainFrame;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> typeFilter;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    public TransactionHistoryPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(true);
        setBackground(HabeshaTheme.BLACK_DEEP);
        setLayout(new BorderLayout());
        add(buildContent(), BorderLayout.CENTER);
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        content.add(buildPageHeader(),  BorderLayout.NORTH);
        content.add(buildFilterBar(),   BorderLayout.CENTER);

        // Table placed via wrapper
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.add(buildTransactionTable(), BorderLayout.CENTER);

        JPanel mid = new JPanel(new BorderLayout());
        mid.setOpaque(false);
        mid.add(buildFilterBar(),  BorderLayout.NORTH);
        mid.add(tableWrapper,      BorderLayout.CENTER);

        content.add(mid, BorderLayout.CENTER);
        return content;
    }

    private JPanel buildPageHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("Transaction History");
        title.setFont(HabeshaTheme.FONT_DISPLAY);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);

        JLabel sub = new JLabel("Full record of your account activity");
        sub.setFont(HabeshaTheme.FONT_SUBHEAD);
        sub.setForeground(HabeshaTheme.CREAM_DIM);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        GoldButton exportBtn = new GoldButton("Export PDF", GoldButton.Style.OUTLINE);
        exportBtn.setPreferredSize(new Dimension(130, 36));
        exportBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "PDF export will be implemented with the PDF receipt module.",
                "Coming Soon", JOptionPane.INFORMATION_MESSAGE));

        panel.add(left,      BorderLayout.WEST);
        panel.add(exportBtn, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bar.setOpaque(false);

        // Search box
        searchField = new JTextField(20);
        searchField.setFont(HabeshaTheme.FONT_BODY);
        searchField.setBackground(HabeshaTheme.BLACK_CARD);
        searchField.setForeground(HabeshaTheme.CREAM_LIGHT);
        searchField.setCaretColor(HabeshaTheme.GOLD_PRIMARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HabeshaTheme.BLACK_BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        searchField.setPreferredSize(new Dimension(220, 36));
        bar.add(searchField);

        // Type filter
        String[] types = { "All Types", "Deposit", "Withdrawal", "Transfer In", "Transfer Out", "Equb", "Iddir" };
        typeFilter = new JComboBox<>(types);
        typeFilter.setFont(HabeshaTheme.FONT_BODY);
        typeFilter.setPreferredSize(new Dimension(150, 36));
        bar.add(typeFilter);

        // Date range combos
        String[] months = { "All Months", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        JComboBox<String> monthFilter = new JComboBox<>(months);
        monthFilter.setFont(HabeshaTheme.FONT_BODY);
        monthFilter.setPreferredSize(new Dimension(120, 36));
        bar.add(monthFilter);

        GoldButton searchBtn = new GoldButton("Filter");
        searchBtn.setPreferredSize(new Dimension(90, 36));
        searchBtn.addActionListener(e -> applyFilter());
        bar.add(searchBtn);

        GoldButton clearBtn = new GoldButton("Clear", GoldButton.Style.OUTLINE);
        clearBtn.setPreferredSize(new Dimension(80, 36));
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            typeFilter.setSelectedIndex(0);
            monthFilter.setSelectedIndex(0);
        });
        bar.add(clearBtn);

        return bar;
    }

    private JScrollPane buildTransactionTable() {
        String[] cols = { "Ref #", "Date & Time", "Description", "Type", "Amount", "Balance After" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        loadDemoTransactions();

        JTable table = new JTable(tableModel);
        table.setRowHeight(42);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 4));
        table.setBackground(HabeshaTheme.BLACK_CARD);
        table.setForeground(HabeshaTheme.CREAM_MID);
        table.setFont(HabeshaTheme.FONT_BODY);
        table.setSelectionBackground(HabeshaTheme.GOLD_GLOW);
        table.setSelectionForeground(HabeshaTheme.CREAM_LIGHT);
        table.getTableHeader().setFont(HabeshaTheme.FONT_BODY_BOLD);
        table.getTableHeader().setBackground(HabeshaTheme.BLACK_PANEL);
        table.getTableHeader().setForeground(HabeshaTheme.GOLD_PRIMARY);
        table.getTableHeader().setReorderingAllowed(false);

        // Amount coloring
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String text = val != null ? val.toString() : "";
                lbl.setForeground(text.startsWith("+") ? HabeshaTheme.GREEN_SUCCESS : HabeshaTheme.RED_DANGER);
                lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                lbl.setFont(HabeshaTheme.FONT_BODY_BOLD);
                return lbl;
            }
        });

        // Right-align balance
        DefaultTableCellRenderer rightAlign = new DefaultTableCellRenderer();
        rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(5).setCellRenderer(rightAlign);

        // Monospace ref column
        DefaultTableCellRenderer monoRend = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                lbl.setFont(HabeshaTheme.FONT_MONO);
                lbl.setForeground(HabeshaTheme.GOLD_MUTED);
                return lbl;
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(monoRend);

        int[] widths = { 120, 150, 240, 110, 120, 130 };
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(HabeshaTheme.BLACK_BORDER));
        scroll.setBackground(HabeshaTheme.BLACK_CARD);
        scroll.getViewport().setBackground(HabeshaTheme.BLACK_CARD);
        return scroll;
    }

    private void loadDemoTransactions() {
        Object[][] rows = {
                { "HB892341234", LocalDateTime.now().minusHours(2).format(FMT),   "Cash Deposit",                "DEPOSIT",     "+5,000.00 ETB",  "47,850.00 ETB" },
                { "HB881200045", LocalDateTime.now().minusDays(1).format(FMT),    "Transfer to Kaleb Girma",    "TRANSFER OUT","-2,500.00 ETB",  "42,850.00 ETB" },
                { "HB871905561", LocalDateTime.now().minusDays(2).format(FMT),    "Equb Round #7",              "EQUB",        "-500.00 ETB",    "45,350.00 ETB" },
                { "HB860044209", LocalDateTime.now().minusDays(3).format(FMT),    "ATM Withdrawal – Bole",      "WITHDRAWAL",  "-700.00 ETB",    "45,850.00 ETB" },
                { "HB851133871", LocalDateTime.now().minusDays(5).format(FMT),    "Salary Credit – Ethio Telecom","DEPOSIT",   "+10,000.00 ETB", "46,550.00 ETB" },
                { "HB840098234", LocalDateTime.now().minusDays(6).format(FMT),    "Iddir Contribution – Round 3","IDDIR",      "-300.00 ETB",    "36,550.00 ETB" },
                { "HB831000892", LocalDateTime.now().minusDays(8).format(FMT),    "Transfer from Meron Tadesse","TRANSFER IN", "+3,000.00 ETB",  "36,850.00 ETB" },
                { "HB820765401", LocalDateTime.now().minusDays(10).format(FMT),   "ATM Withdrawal – Piassa",    "WITHDRAWAL",  "-1,500.00 ETB",  "33,850.00 ETB" },
                { "HB810234789", LocalDateTime.now().minusDays(12).format(FMT),   "Cash Deposit",               "DEPOSIT",     "+8,000.00 ETB",  "35,350.00 ETB" },
                { "HB800456123", LocalDateTime.now().minusDays(14).format(FMT),   "Equb Round #6",              "EQUB",        "-500.00 ETB",    "27,350.00 ETB" },
        };
        for (Object[] row : rows) tableModel.addRow(row);
    }

    private void applyFilter() {
        // Filter logic will be implemented with data layer
        JOptionPane.showMessageDialog(this,
                "Advanced filtering will be wired to the database layer.",
                "Filter", JOptionPane.INFORMATION_MESSAGE);
    }
}