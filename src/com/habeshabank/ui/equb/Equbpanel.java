package com.habeshabank.ui.equb;

import com.habeshabank.exception.BankingException;
import com.habeshabank.exception.ValidationException;
import com.habeshabank.model.EqubGroup;
import com.habeshabank.model.EqubRound;
import com.habeshabank.service.EqubService;
import com.habeshabank.service.TransactionService;
import com.habeshabank.ui.Refreshable;
import com.habeshabank.ui.components.*;
import com.habeshabank.ui.dashboard.MainFrame;
import com.habeshabank.ui.theme.HabeshaTheme;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Digital Equb panel — Phase 7 (full implementation).
 *
 * Replaced
 * ────────
 * • Static hardcoded group cards ("Merkato Circle", "Bole Savers") → live cards
 *   from EqubService.getMyGroups()
 * • Static round history table → live from EqubService.getMyRoundHistory()
 * • showComingSoon() on Create/Join → real styled dialogs
 *
 * Preserved
 * ─────────
 * • Page header (title, subtitle)
 * • Card visual style (BLACK_CARD, accent bar, rounded corners)
 * • Table styling (GOLD_PRIMARY headers, BLACK_CARD background)
 * • Button layout and GoldButton styles
 * • Refreshable contract
 */
public class EqubPanel extends JPanel implements Refreshable {

    private final MainFrame mainFrame;
    private final EqubService equbService = EqubService.getInstance();
    private final TransactionService txService =
            TransactionService.getInstance();

    // Live UI components updated by refreshData()
    private JPanel groupCardsPanel;
    private DefaultTableModel roundTableModel;
    private JLabel totalContribLabel;

    public EqubPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(true);
        setBackground(HabeshaTheme.BLACK_DEEP);
        setLayout(new BorderLayout());
        add(buildScrollableContent(), BorderLayout.CENTER);
    }

    // ── Refreshable ───────────────────────────────────────────────────────────

    @Override
    public void refreshData() {
        refreshGroupCards();
        refreshRoundTable();
        if (totalContribLabel != null) {
            long count = txService.getEqubContributionCount();
            totalContribLabel.setText(
                    "Total contributions this session: " + count
            );
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private JScrollPane buildScrollableContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        content.add(buildPageHeader());
        content.add(Box.createVerticalStrut(24));
        content.add(buildGroupCardsSection());
        content.add(Box.createVerticalStrut(24));
        content.add(buildRoundHistorySection());
        content.add(Box.createVerticalStrut(24));
        content.add(buildActionBar());

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

        JLabel sub = new JLabel(
                "Community rotating savings — digitally managed and transparent"
        );
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

    // ── Group cards (live) ────────────────────────────────────────────────────

    private JPanel buildGroupCardsSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);

        JLabel header = new JLabel("My Equb Groups");
        header.setFont(HabeshaTheme.FONT_HEADING);
        header.setForeground(HabeshaTheme.CREAM_LIGHT);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        section.add(header, BorderLayout.NORTH);

        groupCardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        groupCardsPanel.setOpaque(false);
        refreshGroupCards();

        section.add(groupCardsPanel, BorderLayout.CENTER);
        return section;
    }

    private void refreshGroupCards() {
        if (groupCardsPanel == null) return;
        groupCardsPanel.removeAll();

        List<EqubGroup> groups = equbService.getMyGroups();
        if (groups.isEmpty()) {
            JLabel empty = new JLabel(
                    "You have not joined any Equb groups yet."
            );
            empty.setFont(HabeshaTheme.FONT_BODY);
            empty.setForeground(HabeshaTheme.CREAM_DIM);
            groupCardsPanel.add(empty);
        } else {
            for (EqubGroup g : groups) {
                groupCardsPanel.add(buildLiveGroupCard(g));
            }
        }

        groupCardsPanel.revalidate();
        groupCardsPanel.repaint();
    }

    private JPanel buildLiveGroupCard(EqubGroup group) {
        Color accent = group.isActive()
                ? HabeshaTheme.GOLD_PRIMARY
                : HabeshaTheme.CREAM_DIM;

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );
                g2.setColor(HabeshaTheme.BLACK_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.fillRoundRect(0, 0, getWidth() - 1, 4, 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setPreferredSize(new Dimension(210, 170));

        JLabel nameLabel = new JLabel(group.getName());
        nameLabel.setFont(HabeshaTheme.FONT_HEADING);
        nameLabel.setForeground(HabeshaTheme.CREAM_LIGHT);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(6));

        JLabel amtLabel = new JLabel(
                String.format("%,.0f ETB/round", group.getContributionAmount())
        );
        amtLabel.setFont(HabeshaTheme.FONT_AMOUNT);
        amtLabel.setForeground(accent);
        amtLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(amtLabel);
        card.add(Box.createVerticalStrut(6));

        String roundText = group.isCompleted()
                ? "Completed"
                : "Round " +
                group.getCurrentRound() +
                  " of " +
                group.getTotalRounds();
        JLabel roundLabel = new JLabel(roundText);
        roundLabel.setFont(HabeshaTheme.FONT_SMALL);
        roundLabel.setForeground(HabeshaTheme.CREAM_DIM);
        roundLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(roundLabel);

        JLabel codeLabel = new JLabel("Code: " + group.getGroupCode());
        codeLabel.setFont(HabeshaTheme.FONT_MONO);
        codeLabel.setForeground(HabeshaTheme.GOLD_MUTED);
        codeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(codeLabel);
        card.add(Box.createVerticalStrut(10));

        if (group.isActive()) {
            GoldButton contributeBtn = new GoldButton(
                    "Contribute",
                    GoldButton.Style.OUTLINE
            );
            contributeBtn.setPreferredSize(new Dimension(160, 32));
            contributeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            contributeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            contributeBtn.addActionListener(e -> handleContribute(group));
            card.add(contributeBtn);
        }

        return card;
    }

    // ── Round history table (live) ────────────────────────────────────────────

    private JPanel buildRoundHistorySection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);

        JLabel title = new JLabel("Round History");
        title.setFont(HabeshaTheme.FONT_HEADING);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        section.add(title, BorderLayout.NORTH);

        String[] cols = {
                "Group",
                "Round",
                "Contribution",
                "Pot Amount",
                "Winner",
                "Status",
        };
        roundTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(roundTableModel);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 4));
        table.setBackground(HabeshaTheme.BLACK_CARD);
        table.setForeground(HabeshaTheme.CREAM_MID);
        table.setFont(HabeshaTheme.FONT_BODY);
        table.getTableHeader().setFont(HabeshaTheme.FONT_BODY_BOLD);
        table.getTableHeader().setBackground(HabeshaTheme.BLACK_PANEL);
        table.getTableHeader().setForeground(HabeshaTheme.GOLD_PRIMARY);
        table.getTableHeader().setReorderingAllowed(false);

        // Status column colouring
        table.getColumnModel().getColumn(5).setCellRenderer(
                new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable t,
                            Object val,
                            boolean sel,
                            boolean foc,
                            int row,
                            int col
                    ) {
                        JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                                t,
                                val,
                                sel,
                                foc,
                                row,
                                col
                        );
                        String s = val != null ? val.toString() : "";
                        lbl.setForeground(
                                s.equals("PAID")
                                        ? HabeshaTheme.GREEN_SUCCESS
                                        : HabeshaTheme.GOLD_MUTED
                        );
                        return lbl;
                    }
                }
        );

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(
                BorderFactory.createLineBorder(HabeshaTheme.BLACK_BORDER)
        );
        scroll.getViewport().setBackground(HabeshaTheme.BLACK_CARD);
        scroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));
        section.add(scroll, BorderLayout.CENTER);

        refreshRoundTable();
        return section;
    }

    private void refreshRoundTable() {
        if (roundTableModel == null) return;
        roundTableModel.setRowCount(0);

        List<EqubRound> rounds = equbService.getMyRoundHistory();
        if (rounds.isEmpty()) {
            roundTableModel.addRow(new Object[] {
                    "—",
                    "—",
                    "—",
                    "—",
                    "No rounds yet",
                    "—",
            });
            return;
        }
        for (EqubRound r : rounds) {
            String winner = r.isPaid()
                    ? (r.getWinnerName() != null ? r.getWinnerName() : "—")
                    : "Pending draw";
            roundTableModel.addRow(new Object[] {
                    r.getGroupName() != null ? r.getGroupName() : "—",
                    "Round " + r.getRoundNumber(),
                    String.format(
                            "%,.0f ETB",
                            r.getPotAmount() / Math.max(1, r.getContributionsIn())
                    ),
                    String.format("%,.2f ETB", r.getPotAmount()),
                    winner,
                    r.getStatus().name(),
            });
        }
    }

    // ── Action bar ────────────────────────────────────────────────────────────

    private JPanel buildActionBar() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        totalContribLabel = new JLabel("Total contributions this session: 0");
        totalContribLabel.setFont(HabeshaTheme.FONT_SMALL);
        totalContribLabel.setForeground(HabeshaTheme.GOLD_MUTED);
        totalContribLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalContribLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 10, 0)
        );
        wrapper.add(totalContribLabel);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setOpaque(false);

        GoldButton createBtn = new GoldButton("+ Create New Equb");
        createBtn.setPreferredSize(new Dimension(180, 42));
        createBtn.addActionListener(e -> showCreateDialog());
        row.add(createBtn);

        GoldButton joinBtn = new GoldButton(
                "Join with Code",
                GoldButton.Style.OUTLINE
        );
        joinBtn.setPreferredSize(new Dimension(160, 42));
        joinBtn.addActionListener(e -> showJoinDialog());
        row.add(joinBtn);

        wrapper.add(row);
        return wrapper;
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private void showCreateDialog() {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 8));
        form.setBackground(HabeshaTheme.BLACK_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField nameField = styledField("e.g. Bole Savers Circle");
        JTextField amountField = styledField("e.g. 500");
        JTextField roundsField = styledField("e.g. 12");

        form.add(labelFor("Group Name:"));
        form.add(nameField);
        form.add(labelFor("Contribution (ETB):"));
        form.add(amountField);
        form.add(labelFor("Number of Rounds:"));
        form.add(roundsField);

        int result = JOptionPane.showConfirmDialog(
                this,
                form,
                "Create New Equb Group",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        try {
            String name = nameField.getText().trim();
            double amount = Double.parseDouble(
                    amountField.getText().trim().replace(",", "")
            );
            int rounds = Integer.parseInt(roundsField.getText().trim());

            EqubGroup group = equbService.createGroup(name, amount, rounds);

            JOptionPane.showMessageDialog(
                    this,
                    "✓  Equb group \"" +
                            group.getName() +
                            "\" created!\n\n" +
                            "Invite code: " +
                            group.getGroupCode() +
                            "\n" +
                            "Share this code with members to let them join.",
                    "Group Created",
                    JOptionPane.INFORMATION_MESSAGE
            );

            mainFrame.refreshAllUI();
            mainFrame.resetSessionTimeout();
        } catch (NumberFormatException ex) {
            showError(
                    "Invalid number — please check contribution amount and round count."
            );
        } catch (ValidationException ex) {
            showError(ex.getMessage());
        }
    }

    private void showJoinDialog() {
        JTextField codeField = styledField("6-character code, e.g. AB3XY2");

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 8));
        form.setBackground(HabeshaTheme.BLACK_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        form.add(labelFor("Invite Code:"));
        form.add(codeField);

        int result = JOptionPane.showConfirmDialog(
                this,
                form,
                "Join Equb Group",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        try {
            EqubGroup group = equbService.joinGroup(codeField.getText().trim());
            JOptionPane.showMessageDialog(
                    this,
                    "✓  You have joined \"" +
                            group.getName() +
                            "\"!\n" +
                            "Contribution per round: " +
                            String.format("%,.2f ETB", group.getContributionAmount()),
                    "Joined Group",
                    JOptionPane.INFORMATION_MESSAGE
            );

            mainFrame.refreshAllUI();
            mainFrame.resetSessionTimeout();
        } catch (ValidationException ex) {
            showError(ex.getMessage());
        }
    }

    private void handleContribute(EqubGroup group) {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format(
                        "Contribute %,.2f ETB to \"%s\" — Round %d?",
                        group.getContributionAmount(),
                        group.getName(),
                        group.getCurrentRound()
                ),
                "Confirm Contribution",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            equbService.contribute(group.getId());

            JOptionPane.showMessageDialog(
                    this,
                    String.format(
                            "✓  Contribution of %,.2f ETB recorded for \"%s\".",
                            group.getContributionAmount(),
                            group.getName()
                    ),
                    "Contributed",
                    JOptionPane.INFORMATION_MESSAGE
            );

            mainFrame.refreshAllUI();
            mainFrame.resetSessionTimeout();
        } catch (BankingException ex) {
            showError(ex.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField(20);
        f.setFont(HabeshaTheme.FONT_BODY);
        f.setBackground(HabeshaTheme.BLACK_PANEL);
        f.setForeground(HabeshaTheme.CREAM_LIGHT);
        f.setCaretColor(HabeshaTheme.GOLD_PRIMARY);
        f.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(HabeshaTheme.BLACK_BORDER),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)
                )
        );
        return f;
    }

    private JLabel labelFor(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(HabeshaTheme.FONT_SMALL);
        lbl.setForeground(HabeshaTheme.CREAM_DIM);
        return lbl;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(
                this,
                msg,
                "Error",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
