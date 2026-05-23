package com.habeshabank.ui.withdraw;

import com.habeshabank.model.UserSession;
import com.habeshabank.ui.components.*;
import com.habeshabank.ui.dashboard.MainFrame;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Withdraw funds panel. UI shell — service layer to be wired later.
 */
public class WithdrawPanel extends JPanel {

    private final MainFrame mainFrame;
    private HabeshaTextField amountField;
    private JComboBox<String> methodCombo;
    private HabeshaTextField pinField;

    public WithdrawPanel(MainFrame mainFrame) {
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
        content.add(Box.createVerticalStrut(28));

        JPanel columns = new JPanel(new GridLayout(1, 2, 24, 0));
        columns.setOpaque(false);
        columns.add(buildWithdrawForm());
        columns.add(buildSummaryPanel());
        content.add(columns);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    private JPanel buildPageHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Withdraw Funds");
        title.setFont(HabeshaTheme.FONT_DISPLAY);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);

        JLabel sub = new JLabel("Withdraw cash or transfer to external account");
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

    private JPanel buildWithdrawForm() {
        JPanel card = new SectionPanel("", null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel formTitle = new JLabel("Withdrawal Details");
        formTitle.setFont(HabeshaTheme.FONT_HEADING);
        formTitle.setForeground(HabeshaTheme.GOLD_PRIMARY);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(formTitle);
        card.add(Box.createVerticalStrut(20));

        card.add(fieldLabel("Amount (ETB)"));
        card.add(Box.createVerticalStrut(6));
        amountField = new HabeshaTextField("e.g. 2000.00", 20);
        amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        amountField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(amountField);
        card.add(Box.createVerticalStrut(20));

        card.add(fieldLabel("Withdrawal Method"));
        card.add(Box.createVerticalStrut(6));
        String[] methods = { "ATM / Cash", "Bank Counter", "Mobile Money Transfer" };
        methodCombo = new JComboBox<>(methods);
        methodCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        methodCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        methodCombo.setFont(HabeshaTheme.FONT_BODY);
        card.add(methodCombo);
        card.add(Box.createVerticalStrut(20));

        card.add(fieldLabel("Confirm PIN"));
        card.add(Box.createVerticalStrut(6));
        pinField = new HabeshaTextField("Enter your 4-digit PIN", 20);
        pinField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        pinField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(pinField);
        card.add(Box.createVerticalStrut(28));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        GoldButton submitBtn = new GoldButton("Confirm Withdrawal", GoldButton.Style.DANGER);
        submitBtn.setPreferredSize(new Dimension(200, 42));
        submitBtn.addActionListener(e -> handleWithdraw());
        btnRow.add(submitBtn);

        GoldButton clearBtn = new GoldButton("Clear", GoldButton.Style.OUTLINE);
        clearBtn.setPreferredSize(new Dimension(100, 42));
        clearBtn.addActionListener(e -> clearForm());
        btnRow.add(clearBtn);

        card.add(btnRow);
        return card;
    }

    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        StatCard bc = new StatCard("Current Balance",
                String.format("%.2f ETB", UserSession.getInstance().getBalance()),
                "Available to withdraw", HabeshaTheme.GOLD_PRIMARY);
        bc.setAlignmentX(Component.LEFT_ALIGNMENT);
        bc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        panel.add(bc);
        panel.add(Box.createVerticalStrut(16));

        JPanel info = new SectionPanel("", null);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel infoTitle = new JLabel("Withdrawal Limits");
        infoTitle.setFont(HabeshaTheme.FONT_HEADING);
        infoTitle.setForeground(HabeshaTheme.GOLD_PRIMARY);
        infoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(infoTitle);
        info.add(Box.createVerticalStrut(12));

        String[] bullets = {
                "• Daily ATM limit: 10,000 ETB",
                "• Counter withdrawal: up to 100,000 ETB",
                "• Minimum withdrawal: 200 ETB",
                "• PIN verification required",
                "• Receipt available on request"
        };
        for (String b : bullets) {
            JLabel lbl = new JLabel(b);
            lbl.setFont(HabeshaTheme.FONT_BODY);
            lbl.setForeground(HabeshaTheme.CREAM_MID);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            info.add(lbl);
            info.add(Box.createVerticalStrut(6));
        }
        panel.add(info);
        return panel;
    }

    private void handleWithdraw() {
        String amtText = amountField.getText().trim();
        if (amtText.isEmpty()) { showError("Please enter an amount."); return; }
        if (pinField.getText().trim().isEmpty()) { showError("Please enter your PIN."); return; }

        try {
            double amount = Double.parseDouble(amtText.replace(",", ""));
            UserSession session = UserSession.getInstance();

            if (amount < 200)                   { showError("Minimum withdrawal is 200 ETB."); return; }
            if (amount > session.getBalance())   { showError("Insufficient balance."); return; }

            session.setBalance(session.getBalance() - amount);

            JOptionPane.showMessageDialog(this,
                    String.format("✓  Withdrawal of %.2f ETB successful!\nNew Balance: %.2f ETB",
                            amount, session.getBalance()),
                    "Withdrawal Successful", JOptionPane.INFORMATION_MESSAGE);

            clearForm();
        } catch (NumberFormatException ex) {
            showError("Invalid amount.");
        }
    }

    private void clearForm() {
        amountField.setText("");
        methodCombo.setSelectedIndex(0);
        pinField.setText("");
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