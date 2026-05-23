package com.habeshabank.ui.iddir;

import com.habeshabank.ui.components.*;
import com.habeshabank.ui.dashboard.MainFrame;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Iddir (mutual-aid funeral association) contribution simulation panel.
 * UI shell — business logic to be wired later.
 */
public class IddirPanel extends JPanel {

    private final MainFrame mainFrame;
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

    private JScrollPane buildScrollableContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        content.add(buildPageHeader());
        content.add(Box.createVerticalStrut(24));

        JPanel columns = new JPanel(new GridLayout(1, 2, 24, 0));
        columns.setOpaque(false);
        columns.add(buildContributionForm());
        columns.add(buildIddirStatus());
        content.add(columns);
        content.add(Box.createVerticalStrut(24));
        content.add(buildMemberTable());

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

    private JPanel buildContributionForm() {
        JPanel card = new SectionPanel("", null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel formTitle = new JLabel("New Contribution");
        formTitle.setFont(HabeshaTheme.FONT_HEADING);
        formTitle.setForeground(HabeshaTheme.GOLD_PRIMARY);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(formTitle);
        card.add(Box.createVerticalStrut(20));

        card.add(fieldLabel("Member / Beneficiary"));
        card.add(Box.createVerticalStrut(6));
        memberField = new HabeshaTextField("Full name of the beneficiary", 20);
        memberField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        memberField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(memberField);
        card.add(Box.createVerticalStrut(16));

        card.add(fieldLabel("Contribution Amount (ETB)"));
        card.add(Box.createVerticalStrut(6));
        amountField = new HabeshaTextField("e.g. 300.00", 20);
        amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        amountField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(amountField);
        card.add(Box.createVerticalStrut(16));

        card.add(fieldLabel("Purpose / Occasion"));
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

        GoldButton submitBtn = new GoldButton("Submit Contribution");
        submitBtn.setPreferredSize(new Dimension(190, 42));
        submitBtn.addActionListener(e -> handleContribution());
        btnRow.add(submitBtn);

        GoldButton clearBtn = new GoldButton("Clear", GoldButton.Style.OUTLINE);
        clearBtn.setPreferredSize(new Dimension(100, 42));
        clearBtn.addActionListener(e -> clearForm());
        btnRow.add(clearBtn);

        card.add(btnRow);
        return card;
    }

    private JPanel buildIddirStatus() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Status cards
        StatCard membersCard = new StatCard("Active Members", "24",
                "In your Iddir group", HabeshaTheme.GOLD_PRIMARY);
        membersCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        membersCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        panel.add(membersCard);
        panel.add(Box.createVerticalStrut(12));

        StatCard poolCard = new StatCard("Pool Balance", "7,200 ETB",
                "Collective fund", HabeshaTheme.GREEN_SUCCESS);
        poolCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        poolCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        panel.add(poolCard);
        panel.add(Box.createVerticalStrut(12));

        StatCard myCard = new StatCard("My Contributions", "1,800 ETB",
                "Total paid this year", HabeshaTheme.BLUE_INFO);
        myCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        myCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        panel.add(myCard);

        return panel;
    }

    private JPanel buildMemberTable() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);

        JLabel title = new JLabel("Recent Iddir Events");
        title.setFont(HabeshaTheme.FONT_HEADING);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        section.add(title, BorderLayout.NORTH);

        String[] cols = { "Date", "Beneficiary", "Occasion", "Your Contribution", "Group Total", "Status" };
        Object[][] data = {
                { "10 May 2026", "Ato Bekele Tadesse", "Bereavement Support", "300 ETB", "7,200 ETB", "Distributed" },
                { "22 Apr 2026", "W/ro Mulu Girma",    "Medical Aid",          "300 ETB", "6,900 ETB", "Distributed" },
                { "05 Mar 2026", "Ato Haile Wolde",    "Bereavement Support",  "300 ETB", "7,200 ETB", "Distributed" },
                { "18 Feb 2026", "W/t Sofia Alemu",    "Wedding Support",      "300 ETB", "7,200 ETB", "Distributed" },
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

    private void handleContribution() {
        if (memberField.getText().trim().isEmpty())  { showError("Please enter the beneficiary name."); return; }
        if (amountField.getText().trim().isEmpty())  { showError("Please enter an amount."); return; }

        try {
            double amount = Double.parseDouble(amountField.getText().trim().replace(",", ""));
            if (amount <= 0) { showError("Amount must be positive."); return; }

            JOptionPane.showMessageDialog(this,
                    String.format("✓  Iddir contribution of %.2f ETB recorded for %s.",
                            amount, memberField.getText().trim()),
                    "Contribution Recorded", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
        } catch (NumberFormatException ex) {
            showError("Invalid amount.");
        }
    }

    private void clearForm() {
        memberField.setText("");
        amountField.setText("");
        purposeCombo.setSelectedIndex(0);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.WARNING_MESSAGE);
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(HabeshaTheme.FONT_SMALL);
        lbl.setForeground(HabeshaTheme.CREAM_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }
}