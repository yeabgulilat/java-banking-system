package com.habeshabank.ui.history;

import com.habeshabank.model.Transaction;
import com.habeshabank.service.TransactionService;
import com.habeshabank.ui.Refreshable;
import com.habeshabank.ui.components.*;
import com.habeshabank.ui.dashboard.MainFrame;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Transaction history panel — fully reactive.
 *
 * Phase 3 changes
 * ───────────────
 * • Implements {@link Refreshable}; refreshData() clears and reloads the table
 *   from TransactionService on every call.
 * • loadDemoTransactions() removed — hardcoded rows replaced by live data.
 * • Filter button now performs a real in-memory search/filter against the
 *   live transaction list (description keyword + type).
 * • All layout, styling, column renderers, and header unchanged from Phase 1/2.
 */
public class TransactionHistoryPanel extends JPanel implements Refreshable {

    private final MainFrame mainFrame;
    private final TransactionService txService = TransactionService.getInstance();

    private DefaultTableModel tableModel;
    private JTextField        searchField;
    private JComboBox<String> typeFilter;
    private JComboBox<String> monthFilter;

    public TransactionHistoryPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(true);
        setBackground(HabeshaTheme.BLACK_DEEP);
        setLayout(new BorderLayout());
        add(buildContent(), BorderLayout.CENTER);

        // Load live data on construction
        refreshData();
    }

    // ── Refreshable ───────────────────────────────────────────────────────────

    /**
     * Reloads the table from TransactionService with no filters applied.
     * Resets filter controls to "All" so the user sees the complete list.
     */
    @Override
    public void refreshData() {
        if (searchField  != null) searchField.setText("");
        if (typeFilter   != null) typeFilter.setSelectedIndex(0);
        if (monthFilter  != null) monthFilter.setSelectedIndex(0);
        populateTable(txService.getTransactionHistory());
    }

    /** Replaces all table rows with the supplied transaction list. */
    private void populateTable(List<Transaction> transactions) {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        if (transactions.isEmpty()) {
            tableModel.addRow(new Object[]{
                    "—", "—", "No transactions found", "—", "—", "—"
            });
            return;
        }
        for (Transaction tx : transactions) {
            tableModel.addRow(new Object[]{
                    tx.getReferenceNumber(),
                    tx.formattedTimestamp(),
                    tx.getDescription(),
                    tx.getType().name().replace("_", " "),
                    tx.signedAmount(),
                    String.format("%,.2f ETB", tx.getBalanceAfter())
            });
        }
    }

    // ── Layout (structure identical to Phase 1) ───────────────────────────────

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.add(buildTransactionTable(), BorderLayout.CENTER);

        JPanel mid = new JPanel(new BorderLayout());
        mid.setOpaque(false);
        mid.add(buildFilterBar(), BorderLayout.NORTH);
        mid.add(tableWrapper,    BorderLayout.CENTER);

        content.add(buildPageHeader(), BorderLayout.NORTH);
        content.add(mid,               BorderLayout.CENTER);
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

        String[] types = { "All Types", "DEPOSIT", "WITHDRAWAL", "TRANSFER IN",
                "TRANSFER OUT", "EQUB", "IDDIR" };
        typeFilter = new JComboBox<>(types);
        typeFilter.setFont(HabeshaTheme.FONT_BODY);
        typeFilter.setPreferredSize(new Dimension(150, 36));
        bar.add(typeFilter);

        String[] months = { "All Months", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        monthFilter = new JComboBox<>(months);
        monthFilter.setFont(HabeshaTheme.FONT_BODY);
        monthFilter.setPreferredSize(new Dimension(120, 36));
        bar.add(monthFilter);

        GoldButton filterBtn = new GoldButton("Filter");
        filterBtn.setPreferredSize(new Dimension(90, 36));
        filterBtn.addActionListener(e -> applyFilter());
        bar.add(filterBtn);

        GoldButton clearBtn = new GoldButton("Clear", GoldButton.Style.OUTLINE);
        clearBtn.setPreferredSize(new Dimension(80, 36));
        clearBtn.addActionListener(e -> refreshData());
        bar.add(clearBtn);

        return bar;
    }

    private JScrollPane buildTransactionTable() {
        String[] cols = { "Ref #", "Date & Time", "Description", "Type", "Amount", "Balance After" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

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

        // Amount coloring (col 4)
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

        // Balance right-align (col 5)
        DefaultTableCellRenderer rightAlign = new DefaultTableCellRenderer();
        rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(5).setCellRenderer(rightAlign);

        // Ref # monospace + gold (col 0)
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                lbl.setFont(HabeshaTheme.FONT_MONO);
                lbl.setForeground(HabeshaTheme.GOLD_MUTED);
                return lbl;
            }
        });

        int[] widths = { 120, 150, 240, 110, 120, 130 };
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(HabeshaTheme.BLACK_BORDER));
        scroll.setBackground(HabeshaTheme.BLACK_CARD);
        scroll.getViewport().setBackground(HabeshaTheme.BLACK_CARD);
        return scroll;
    }

    // ── Filter logic ──────────────────────────────────────────────────────────

    /**
     * Filters the live transaction list by keyword and/or type and reloads the table.
     * All matching is case-insensitive. An empty keyword or "All Types" matches everything.
     */
    private void applyFilter() {
        String keyword   = searchField.getText().trim().toLowerCase();
        String typeStr   = typeFilter.getSelectedIndex() == 0
                ? ""
                : ((String) typeFilter.getSelectedItem()).replace(" ", "_");
        int    monthIdx  = monthFilter.getSelectedIndex(); // 0 = all, 1-12 = Jan-Dec

        List<Transaction> all = txService.getTransactionHistory();

        List<Transaction> filtered = all.stream()
                .filter(tx -> {
                    // keyword match against description or reference
                    if (!keyword.isEmpty()) {
                        String desc = tx.getDescription() != null
                                ? tx.getDescription().toLowerCase() : "";
                        String ref  = tx.getReferenceNumber() != null
                                ? tx.getReferenceNumber().toLowerCase() : "";
                        if (!desc.contains(keyword) && !ref.contains(keyword)) return false;
                    }
                    // type match
                    if (!typeStr.isEmpty()) {
                        if (!tx.getType().name().equalsIgnoreCase(typeStr)) return false;
                    }
                    // month match
                    if (monthIdx > 0 && tx.getTimestamp() != null) {
                        if (tx.getTimestamp().getMonthValue() != monthIdx) return false;
                    }
                    return true;
                })
                .toList();

        populateTable(filtered);
    }
}
