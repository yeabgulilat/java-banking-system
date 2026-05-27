package com.habeshabank.ui.equb;

import com.habeshabank.exception.BankingException;
import com.habeshabank.model.Transaction;
import com.habeshabank.service.TransactionService;
import com.habeshabank.ui.Refreshable;
import com.habeshabank.ui.components.*;
import com.habeshabank.ui.dashboard.MainFrame;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Digital Equb panel.
 * Phase 3: implements Refreshable — refreshData() updates the live contribution
 *          count label from TransactionService after every contribution.
 *          All group card layout and static table content unchanged.
 */
public class EqubPanel extends JPanel implements Refreshable {

    private final MainFrame mainFrame;
    private final TransactionService txService = TransactionService.getInstance();

    // Phase 3: live label updated by refreshData()
    private JLabel equbCountLabel;

    public EqubPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(true);
        setBackground(HabeshaTheme.BLACK_DEEP);
        setLayout(new BorderLayout());
        add(buildScrollableContent(), BorderLayout.CENTER);
    }

    // ── Refreshable ───────────────────────────────────────────────────────────

    /** Updates the live contribution count so the panel reflects real totals. */
    @Override
    public void refreshData() {
        if (equbCountLabel != null) {
            long count = txService.getEqubContributionCount();
            equbCountLabel.setText("Total contributions this session: " + count);
        }
    }

    private JScrollPane buildScrollableContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        content.add(buildPageHeader());
        content.add(Box.createVerticalStrut(24));
        content.add(buildActiveEqubCards());
        content.add(Box.createVerticalStrut(24));
        content.add(buildEqubTable());
        content.add(Box.createVerticalStrut(24));
        content.add(buildJoinCreateSection());

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

        JLabel title = new JLabel("Digital Equb  ◎");
        title.setFont(HabeshaTheme.FONT_DISPLAY);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);

        JLabel sub = new JLabel("Community rotating savings — digitally managed and transparent");
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

    private JPanel buildActiveEqubCards() {
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);

        row.add(buildEqubCard("Merkato Circle",  "500 ETB/round", "Round 7 of 12",  "Next: 3 days",  HabeshaTheme.GOLD_PRIMARY));
        row.add(buildEqubCard("Bole Savers",     "1,000 ETB/round","Round 3 of 10", "Next: 8 days",  HabeshaTheme.BLUE_INFO));
        row.add(buildEqubCard("Start New Equb",  "—",             "Create a group", "+ Invite Members", HabeshaTheme.BLACK_BORDER));

        return row;
    }

    private JPanel buildEqubCard(String name, String amount, String round, String next, Color accent) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(HabeshaTheme.BLACK_CARD);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                // Top accent bar
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth()-1, 4, 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.setPreferredSize(new Dimension(200, 140));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(HabeshaTheme.FONT_HEADING);
        nameLabel.setForeground(HabeshaTheme.CREAM_LIGHT);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(8));

        JLabel amtLabel = new JLabel(amount);
        amtLabel.setFont(HabeshaTheme.FONT_AMOUNT);
        amtLabel.setForeground(accent.equals(HabeshaTheme.BLACK_BORDER) ? HabeshaTheme.CREAM_DIM : accent);
        amtLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(amtLabel);
        card.add(Box.createVerticalStrut(6));

        JLabel roundLabel = new JLabel(round);
        roundLabel.setFont(HabeshaTheme.FONT_SMALL);
        roundLabel.setForeground(HabeshaTheme.CREAM_DIM);
        roundLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(roundLabel);

        JLabel nextLabel = new JLabel(next);
        nextLabel.setFont(HabeshaTheme.FONT_SMALL);
        nextLabel.setForeground(accent.equals(HabeshaTheme.BLACK_BORDER)
                ? HabeshaTheme.GOLD_PRIMARY : HabeshaTheme.CREAM_DIM);
        nextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nextLabel);

        return card;
    }

    private JPanel buildEqubTable() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);

        JLabel title = new JLabel("Round History");
        title.setFont(HabeshaTheme.FONT_HEADING);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        section.add(title, BorderLayout.NORTH);

        String[] cols = { "Equb Name", "Round", "Date", "Contribution", "Winner", "Status" };
        Object[][] data = {
                { "Merkato Circle", "Round 6", "12 May 2026", "500 ETB", "Selamawit B.", "Completed" },
                { "Merkato Circle", "Round 7", "15 Jun 2026", "500 ETB", "Pending Draw", "Active"    },
                { "Bole Savers",    "Round 2", "20 Apr 2026", "1,000 ETB","Hirut M.",   "Completed" },
                { "Bole Savers",    "Round 3", "20 May 2026", "1,000 ETB","Pending Draw","Active"   },
        };

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Object[] row : data) model.addRow(row);

        JTable table = new JTable(model);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setBackground(HabeshaTheme.BLACK_CARD);
        table.setForeground(HabeshaTheme.CREAM_MID);
        table.setFont(HabeshaTheme.FONT_BODY);
        table.getTableHeader().setFont(HabeshaTheme.FONT_BODY_BOLD);
        table.getTableHeader().setBackground(HabeshaTheme.BLACK_PANEL);
        table.getTableHeader().setForeground(HabeshaTheme.GOLD_PRIMARY);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(HabeshaTheme.BLACK_BORDER));
        scroll.getViewport().setBackground(HabeshaTheme.BLACK_CARD);
        scroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 180));

        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildJoinCreateSection() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        // Live contribution count — updated by refreshData()
        equbCountLabel = new JLabel("Total contributions this session: 0");
        equbCountLabel.setFont(HabeshaTheme.FONT_SMALL);
        equbCountLabel.setForeground(HabeshaTheme.GOLD_MUTED);
        equbCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        equbCountLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        wrapper.add(equbCountLabel);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setOpaque(false);

        GoldButton createBtn = new GoldButton("+ Create New Equb");
        createBtn.setPreferredSize(new Dimension(180, 42));
        createBtn.addActionListener(e -> showComingSoon("Create Equb Group"));
        row.add(createBtn);

        GoldButton joinBtn = new GoldButton("Join Existing Equb", GoldButton.Style.OUTLINE);
        joinBtn.setPreferredSize(new Dimension(180, 42));
        joinBtn.addActionListener(e -> showComingSoon("Join Equb Group"));
        row.add(joinBtn);

        GoldButton contributeBtn = new GoldButton("Make Contribution", GoldButton.Style.OUTLINE);
        contributeBtn.setPreferredSize(new Dimension(180, 42));
        contributeBtn.addActionListener(e -> handleContribution());
        row.add(contributeBtn);

        wrapper.add(row);
        return wrapper;
    }

    private void handleContribution() {
        // Simple input dialog — full Equb group management UI comes in a later phase
        JTextField equbNameField   = new JTextField("Merkato Circle", 20);
        JTextField amountField     = new JTextField("500", 20);

        JPanel inputPanel = new JPanel(new GridLayout(0, 1, 0, 8));
        inputPanel.setBackground(HabeshaTheme.BLACK_CARD);
        inputPanel.add(new JLabel("Equb Group Name:"));
        inputPanel.add(equbNameField);
        inputPanel.add(new JLabel("Contribution Amount (ETB):"));
        inputPanel.add(amountField);

        int result = JOptionPane.showConfirmDialog(this, inputPanel,
                "Make Equb Contribution", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        String equbName = equbNameField.getText().trim();
        String amtText  = amountField.getText().trim();

        if (equbName.isEmpty()) { showError("Please enter the Equb group name."); return; }
        if (amtText.isEmpty())  { showError("Please enter an amount."); return; }

        try {
            double amount = Double.parseDouble(amtText.replace(",", ""));
            Transaction tx = txService.equbContribution(amount, equbName);

            JOptionPane.showMessageDialog(this,
                    String.format("✓  Equb contribution of %.2f ETB recorded!\nRef: %s",
                            amount, tx.getReferenceNumber()),
                    "Contribution Successful", JOptionPane.INFORMATION_MESSAGE);

            mainFrame.refreshAllUI();
            mainFrame.resetSessionTimeout();

        } catch (NumberFormatException ex) {
            showError("Invalid amount.");
        } catch (BankingException ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.WARNING_MESSAGE);
    }

    private void showComingSoon(String feature) {
        JOptionPane.showMessageDialog(this,
                feature + " will be available in the next development phase.",
                "Coming Soon", JOptionPane.INFORMATION_MESSAGE);
    }
}
