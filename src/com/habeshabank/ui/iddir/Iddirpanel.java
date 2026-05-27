package com.habeshabank.ui.iddir;

import com.habeshabank.exception.BankingException;
import com.habeshabank.exception.ValidationException;
import com.habeshabank.model.IddirEvent;
import com.habeshabank.model.IddirGroup;
import com.habeshabank.service.IddirService;
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
 * Iddir (mutual-aid) panel — Phase 7 (full implementation).
 *
 * Replaced
 * ────────
 * • "Active Members: 24" static StatCard → live count from IddirService
 * • "Pool Balance: 7,200 ETB" static StatCard → live sum from IddirService
 * • "My Contributions: 1,800 ETB" → live from IddirService
 * • Static 4-row event table → live events from IddirService
 * • Static contribution form → now calls IddirService.reportEvent() or
 *   IddirService.contributeToEvent() per event
 *
 * Preserved
 * ─────────
 * • Page header, two-column layout, section card styling
 * • StatCard visual design, table styling
 * • All HabeshaTheme colours and fonts
 * • Refreshable contract
 */
public class IddirPanel extends JPanel implements Refreshable {

    private final MainFrame   mainFrame;
    private final IddirService iddirService = IddirService.getInstance();

    // Live StatCards updated by refreshData()
    private StatCard membersCard;
    private StatCard poolCard;
    private StatCard myContribCard;

    // Live event table
    private DefaultTableModel eventTableModel;

    // Form fields (for "Report New Event")
    private HabeshaTextField memberField;
    private HabeshaTextField amountField;
    private JComboBox<String> purposeCombo;

    public IddirPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(true);
        setBackground(HabeshaTheme.BLACK_DEEP);
        setLayout(new BorderLayout());
        add(buildScrollableContent(), BorderLayout.CENTER);
    }

    // ── Refreshable ───────────────────────────────────────────────────────────

    @Override
    public void refreshData() {
        // Stat cards
        int memberCount = iddirService.getTotalMemberCount();
        if (membersCard != null) {
            membersCard.setValue(String.valueOf(memberCount));
            membersCard.setSubtitle(memberCount == 1 ? "member" : "members");
            membersCard.repaint();
        }
        double pool = iddirService.getTotalPoolBalance();
        if (poolCard != null) {
            poolCard.setValue(pool > 0 ? String.format("%,.2f ETB", pool) : "0.00 ETB");
            poolCard.repaint();
        }
        double myTotal = iddirService.getMyTotalContributions();
        if (myContribCard != null) {
            myContribCard.setValue(myTotal > 0
                    ? String.format("%,.2f ETB", myTotal) : "0.00 ETB");
            myContribCard.repaint();
        }

        // Event table
        refreshEventTable();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private JScrollPane buildScrollableContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        content.add(buildPageHeader());
        content.add(Box.createVerticalStrut(24));

        JPanel columns = new JPanel(new GridLayout(1, 2, 24, 0));
        columns.setOpaque(false);
        columns.add(buildFormCard());
        columns.add(buildStatusPanel());
        content.add(columns);

        content.add(Box.createVerticalStrut(24));
        content.add(buildEventTableSection());
        content.add(Box.createVerticalStrut(24));
        content.add(buildGroupActionBar());

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

        JLabel title = new JLabel("Iddir Support  ♦");
        title.setFont(HabeshaTheme.FONT_DISPLAY);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);

        JLabel sub = new JLabel("Mutual-aid contributions — supporting community members in need");
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

    // ── Left: Report Event form ───────────────────────────────────────────────

    private JPanel buildFormCard() {
        JPanel card = new SectionPanel("", null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel formTitle = new JLabel("Report New Event");
        formTitle.setFont(HabeshaTheme.FONT_HEADING);
        formTitle.setForeground(HabeshaTheme.GOLD_PRIMARY);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(formTitle);
        card.add(Box.createVerticalStrut(6));

        JLabel hint = new JLabel("Report a community event — members will contribute.");
        hint.setFont(HabeshaTheme.FONT_SMALL);
        hint.setForeground(HabeshaTheme.CREAM_DIM);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hint);
        card.add(Box.createVerticalStrut(18));

        card.add(fieldLabel("Member / Beneficiary Name"));
        card.add(Box.createVerticalStrut(6));
        memberField = new HabeshaTextField("Full name of the beneficiary", 20);
        memberField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        memberField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(memberField);
        card.add(Box.createVerticalStrut(16));

        card.add(fieldLabel("Requested Amount (ETB)"));
        card.add(Box.createVerticalStrut(6));
        amountField = new HabeshaTextField("e.g. 7200.00", 20);
        amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        amountField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(amountField);
        card.add(Box.createVerticalStrut(16));

        card.add(fieldLabel("Occasion / Purpose"));
        card.add(Box.createVerticalStrut(6));
        String[] purposes = { "Bereavement Support", "Medical Aid", "Wedding Support",
                "Birth Celebration", "Education Fund", "Emergency Relief" };
        purposeCombo = new JComboBox<>(purposes);
        purposeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        purposeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        purposeCombo.setFont(HabeshaTheme.FONT_BODY);
        card.add(purposeCombo);
        card.add(Box.createVerticalStrut(28));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        GoldButton submitBtn = new GoldButton("Report Event");
        submitBtn.setPreferredSize(new Dimension(170, 42));
        submitBtn.addActionListener(e -> handleReportEvent());
        btnRow.add(submitBtn);

        GoldButton clearBtn = new GoldButton("Clear", GoldButton.Style.OUTLINE);
        clearBtn.setPreferredSize(new Dimension(100, 42));
        clearBtn.addActionListener(e -> clearForm());
        btnRow.add(clearBtn);

        card.add(btnRow);
        return card;
    }

    // ── Right: Live stat cards ────────────────────────────────────────────────

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        int memberCount = iddirService.getTotalMemberCount();
        membersCard = new StatCard("Active Members",
                String.valueOf(memberCount),
                memberCount == 1 ? "member" : "members",
                HabeshaTheme.GOLD_PRIMARY);
        membersCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        membersCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        panel.add(membersCard);
        panel.add(Box.createVerticalStrut(12));

        double pool = iddirService.getTotalPoolBalance();
        poolCard = new StatCard("Pool Balance",
                pool > 0 ? String.format("%,.2f ETB", pool) : "0.00 ETB",
                "Collective fund",
                HabeshaTheme.GREEN_SUCCESS);
        poolCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        poolCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        panel.add(poolCard);
        panel.add(Box.createVerticalStrut(12));

        double myTotal = iddirService.getMyTotalContributions();
        myContribCard = new StatCard("My Contributions",
                myTotal > 0 ? String.format("%,.2f ETB", myTotal) : "0.00 ETB",
                "Total paid (all time)",
                HabeshaTheme.BLUE_INFO);
        myContribCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        myContribCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        panel.add(myContribCard);

        return panel;
    }

    // ── Event table (live) ────────────────────────────────────────────────────

    private JPanel buildEventTableSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);

        JLabel title = new JLabel("Iddir Events");
        title.setFont(HabeshaTheme.FONT_HEADING);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        section.add(title, BorderLayout.NORTH);

        String[] cols = { "Date", "Beneficiary", "Occasion",
                "Requested", "Collected", "Members", "Status" };
        eventTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(eventTableModel);
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
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String s = val != null ? val.toString() : "";
                lbl.setForeground(s.equals("DISTRIBUTED")
                        ? HabeshaTheme.GREEN_SUCCESS : HabeshaTheme.GOLD_MUTED);
                return lbl;
            }
        });

        int[] widths = { 110, 160, 140, 100, 100, 80, 100 };
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(HabeshaTheme.BLACK_BORDER));
        scroll.getViewport().setBackground(HabeshaTheme.BLACK_CARD);
        scroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));
        section.add(scroll, BorderLayout.CENTER);

        refreshEventTable();
        return section;
    }

    private void refreshEventTable() {
        if (eventTableModel == null) return;
        eventTableModel.setRowCount(0);

        List<IddirEvent> events = iddirService.getMyEvents();
        if (events.isEmpty()) {
            eventTableModel.addRow(new Object[]{ "—", "—",
                    "No events yet", "—", "—", "—", "—" });
            return;
        }
        for (IddirEvent ev : events) {
            eventTableModel.addRow(new Object[]{
                    ev.formattedDate(),
                    ev.getBeneficiaryName(),
                    ev.getOccasion(),
                    String.format("%,.2f ETB", ev.getRequestedAmount()),
                    String.format("%,.2f ETB", ev.getTotalCollected()),
                    ev.getContributionCount() + " members",
                    ev.getStatus().name()
            });
        }
    }

    // ── Group action bar ──────────────────────────────────────────────────────

    private JPanel buildGroupActionBar() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setOpaque(false);

        GoldButton createBtn = new GoldButton("+ Create Iddir Group");
        createBtn.setPreferredSize(new Dimension(190, 42));
        createBtn.addActionListener(e -> showCreateDialog());
        row.add(createBtn);

        GoldButton joinBtn = new GoldButton("Join with Code", GoldButton.Style.OUTLINE);
        joinBtn.setPreferredSize(new Dimension(160, 42));
        joinBtn.addActionListener(e -> showJoinDialog());
        row.add(joinBtn);

        return row;
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private void showCreateDialog() {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 8));
        form.setBackground(HabeshaTheme.BLACK_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField nameField   = styledField("e.g. Kebele 05 Iddir");
        JTextField amtField    = styledField("e.g. 300");

        form.add(labelFor("Group Name:"));
        form.add(nameField);
        form.add(labelFor("Contribution per Event (ETB):"));
        form.add(amtField);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Create Iddir Group", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            String name   = nameField.getText().trim();
            double amount = Double.parseDouble(amtField.getText().trim().replace(",", ""));

            IddirGroup group = iddirService.createGroup(name, amount);

            JOptionPane.showMessageDialog(this,
                    "✓  Iddir group \"" + group.getName() + "\" created!\n\n"
                            + "Invite code: " + group.getGroupCode() + "\n"
                            + "Share this code with members to let them join.",
                    "Group Created", JOptionPane.INFORMATION_MESSAGE);

            mainFrame.refreshAllUI();
            mainFrame.resetSessionTimeout();

        } catch (NumberFormatException ex) {
            showError("Invalid amount — please enter a number.");
        } catch (ValidationException ex) {
            showError(ex.getMessage());
        }
    }

    private void showJoinDialog() {
        JTextField codeField = styledField("6-character code");
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 8));
        form.setBackground(HabeshaTheme.BLACK_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        form.add(labelFor("Invite Code:"));
        form.add(codeField);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Join Iddir Group", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            IddirGroup group = iddirService.joinGroup(codeField.getText().trim());
            JOptionPane.showMessageDialog(this,
                    "✓  You have joined \"" + group.getName() + "\"!\n"
                            + "Contribution per event: "
                            + String.format("%,.2f ETB", group.getMonthlyAmount()),
                    "Joined Group", JOptionPane.INFORMATION_MESSAGE);

            mainFrame.refreshAllUI();
            mainFrame.resetSessionTimeout();

        } catch (ValidationException ex) {
            showError(ex.getMessage());
        }
    }

    // ── Form actions ──────────────────────────────────────────────────────────

    private void handleReportEvent() {
        String beneficiary = memberField.getText().trim();
        String amtText     = amountField.getText().trim();
        String occasion    = (String) purposeCombo.getSelectedItem();

        if (beneficiary.isEmpty()) { showError("Please enter the beneficiary name."); return; }
        if (amtText.isEmpty())     { showError("Please enter the requested amount."); return; }

        List<IddirGroup> groups = iddirService.getMyGroups();
        if (groups.isEmpty()) {
            showError("You are not a member of any Iddir group.\n"
                    + "Create or join a group first.");
            return;
        }

        try {
            double amount = Double.parseDouble(amtText.replace(",", ""));

            // If user belongs to multiple groups, let them choose
            IddirGroup targetGroup;
            if (groups.size() == 1) {
                targetGroup = groups.get(0);
            } else {
                String[] groupNames = groups.stream()
                        .map(IddirGroup::getName).toArray(String[]::new);
                String chosen = (String) JOptionPane.showInputDialog(this,
                        "Select the Iddir group for this event:",
                        "Select Group", JOptionPane.PLAIN_MESSAGE,
                        null, groupNames, groupNames[0]);
                if (chosen == null) return;
                targetGroup = groups.stream()
                        .filter(g -> g.getName().equals(chosen))
                        .findFirst().orElse(groups.get(0));
            }

            IddirEvent event = iddirService.reportEvent(
                    targetGroup.getId(), beneficiary, occasion, amount);

            JOptionPane.showMessageDialog(this,
                    String.format("✓  Event reported for %s in \"%s\".\n"
                                    + "Occasion: %s\n"
                                    + "Requested: %,.2f ETB\n\n"
                                    + "Members will be notified to contribute.",
                            beneficiary, targetGroup.getName(),
                            occasion, amount),
                    "Event Reported", JOptionPane.INFORMATION_MESSAGE);

            clearForm();
            mainFrame.refreshAllUI();
            mainFrame.resetSessionTimeout();

        } catch (NumberFormatException ex) {
            showError("Invalid amount.");
        } catch (BankingException ex) {
            showError(ex.getMessage());
        }
    }

    private void clearForm() {
        memberField.setText("");
        amountField.setText("");
        purposeCombo.setSelectedIndex(0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField(20);
        f.setFont(HabeshaTheme.FONT_BODY);
        f.setBackground(HabeshaTheme.BLACK_PANEL);
        f.setForeground(HabeshaTheme.CREAM_LIGHT);
        f.setCaretColor(HabeshaTheme.GOLD_PRIMARY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HabeshaTheme.BLACK_BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return f;
    }

    private JLabel labelFor(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(HabeshaTheme.FONT_SMALL);
        lbl.setForeground(HabeshaTheme.CREAM_DIM);
        return lbl;
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(HabeshaTheme.FONT_SMALL);
        lbl.setForeground(HabeshaTheme.CREAM_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.WARNING_MESSAGE);
    }
}