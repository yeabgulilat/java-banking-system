package com.habeshabank.ui.transfer;

import com.habeshabank.model.UserSession;
import com.habeshabank.ui.components.*;
import com.habeshabank.ui.dashboard.MainFrame;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Fund transfer panel. UI shell — service layer to be wired later.
 */
public class TransferPanel extends JPanel {

    private final MainFrame mainFrame;
    private HabeshaTextField recipientField;
    private HabeshaTextField recipientNameField;
    private HabeshaTextField amountField;
    private JComboBox<String> bankCombo;
    private JTextArea descArea;

    public TransferPanel(MainFrame mainFrame) {
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
        columns.add(buildTransferForm());
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

        JLabel title = new JLabel("Send Money");
        title.setFont(HabeshaTheme.FONT_DISPLAY);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);

        JLabel sub = new JLabel("Transfer funds to any Habesha Bank account or other banks");
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

    private JPanel buildTransferForm() {
        JPanel card = new SectionPanel("", null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel formTitle = new JLabel("Transfer Details");
        formTitle.setFont(HabeshaTheme.FONT_HEADING);
        formTitle.setForeground(HabeshaTheme.GOLD_PRIMARY);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(formTitle);
        card.add(Box.createVerticalStrut(20));

        card.add(fieldLabel("Recipient Bank"));
        card.add(Box.createVerticalStrut(6));
        String[] banks = {
                "Habesha Bank (Internal)",
                "Commercial Bank of Ethiopia",
                "Awash Bank",
                "Abyssinia Bank",
                "Dashen Bank",
                "Berhan Bank"
        };
        bankCombo = new JComboBox<>(banks);
        bankCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        bankCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        bankCombo.setFont(HabeshaTheme.FONT_BODY);
        card.add(bankCombo);
        card.add(Box.createVerticalStrut(16));

        card.add(fieldLabel("Recipient Account Number"));
        card.add(Box.createVerticalStrut(6));
        recipientField = new HabeshaTextField("Account number or IBAN", 20);
        recipientField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        recipientField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(recipientField);
        card.add(Box.createVerticalStrut(16));

        card.add(fieldLabel("Recipient Full Name"));
        card.add(Box.createVerticalStrut(6));
        recipientNameField = new HabeshaTextField("e.g. Kaleb Girma", 20);
        recipientNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        recipientNameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(recipientNameField);
        card.add(Box.createVerticalStrut(16));

        card.add(fieldLabel("Amount (ETB)"));
        card.add(Box.createVerticalStrut(6));
        amountField = new HabeshaTextField("e.g. 3000.00", 20);
        amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        amountField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(amountField);
        card.add(Box.createVerticalStrut(16));

        card.add(fieldLabel("Description / Reference"));
        card.add(Box.createVerticalStrut(6));
        descArea = new JTextArea(2, 20);
        descArea.setFont(HabeshaTheme.FONT_BODY);
        descArea.setBackground(HabeshaTheme.BLACK_CARD);
        descArea.setForeground(HabeshaTheme.CREAM_LIGHT);
        descArea.setCaretColor(HabeshaTheme.GOLD_PRIMARY);
        descArea.setLineWrap(true);
        descArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HabeshaTheme.BLACK_BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JScrollPane ds = new JScrollPane(descArea);
        ds.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        ds.setBorder(BorderFactory.createEmptyBorder());
        ds.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(ds);
        card.add(Box.createVerticalStrut(28));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        GoldButton submitBtn = new GoldButton("Send Transfer");
        submitBtn.setPreferredSize(new Dimension(180, 42));
        submitBtn.addActionListener(e -> handleTransfer());
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
                "Available for transfer", HabeshaTheme.GOLD_PRIMARY);
        bc.setAlignmentX(Component.LEFT_ALIGNMENT);
        bc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        panel.add(bc);
        panel.add(Box.createVerticalStrut(16));

        JPanel info = new SectionPanel("", null);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel infoTitle = new JLabel("Transfer Information");
        infoTitle.setFont(HabeshaTheme.FONT_HEADING);
        infoTitle.setForeground(HabeshaTheme.GOLD_PRIMARY);
        infoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(infoTitle);
        info.add(Box.createVerticalStrut(12));

        String[] bullets = {
                "• Internal transfers: instant",
                "• Inter-bank: 1 business day",
                "• Daily limit: 50,000 ETB",
                "• Fee: Free (internal) / 25 ETB",
                "• Double-check account numbers"
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

    private void handleTransfer() {
        if (recipientField.getText().trim().isEmpty())     { showError("Recipient account is required."); return; }
        if (recipientNameField.getText().trim().isEmpty()) { showError("Recipient name is required."); return; }
        if (amountField.getText().trim().isEmpty())        { showError("Amount is required."); return; }

        try {
            double amount = Double.parseDouble(amountField.getText().trim().replace(",", ""));
            UserSession session = UserSession.getInstance();

            if (amount <= 0)                   { showError("Amount must be positive."); return; }
            if (amount > session.getBalance()) { showError("Insufficient balance."); return; }

            int confirm = JOptionPane.showConfirmDialog(this,
                    String.format("Transfer %.2f ETB to %s?\nAccount: %s",
                            amount, recipientNameField.getText().trim(),
                            recipientField.getText().trim()),
                    "Confirm Transfer", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                session.setBalance(session.getBalance() - amount);
                JOptionPane.showMessageDialog(this,
                        String.format("✓  Transfer of %.2f ETB successful!", amount),
                        "Transfer Sent", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
            }
        } catch (NumberFormatException ex) {
            showError("Invalid amount.");
        }
    }

    private void clearForm() {
        recipientField.setText("");
        recipientNameField.setText("");
        amountField.setText("");
        descArea.setText("");
        bankCombo.setSelectedIndex(0);
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