package com.habeshabank.ui.transfer;

import com.habeshabank.exception.BankingException;
import com.habeshabank.model.Transaction;
import com.habeshabank.model.UserSession;
import com.habeshabank.service.TransactionService;
import com.habeshabank.ui.Refreshable;
import com.habeshabank.ui.components.*;
import com.habeshabank.ui.dashboard.MainFrame;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Fund transfer panel.
 * Phase 2: handleTransfer() delegates to TransactionService.
 * Phase 3: implements Refreshable — refreshData() updates the balance StatCard.
 *          All layout and styling unchanged from Phase 1/2.
 */
public class TransferPanel extends JPanel implements Refreshable {

    private final MainFrame mainFrame;
    private HabeshaTextField recipientField;
    private HabeshaTextField recipientNameField;
    private HabeshaTextField amountField;
    private JComboBox<String> bankCombo;
    private JTextArea descArea;

    // Phase 3: promoted to field so refreshData() can update it
    private StatCard balanceCard;

    private final TransactionService txService = TransactionService.getInstance();

    public TransferPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(true);
        setBackground(HabeshaTheme.BLACK_DEEP);
        setLayout(new BorderLayout());
        add(buildScrollableContent(), BorderLayout.CENTER);
    }

    // ── Refreshable ───────────────────────────────────────────────────────────

    @Override
    public void refreshData() {
        if (balanceCard != null) {
            balanceCard.setValue(String.format("%.2f ETB", UserSession.getInstance().getBalance()));
            balanceCard.repaint();
        }
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

        balanceCard = new StatCard("Current Balance",
                String.format("%.2f ETB", UserSession.getInstance().getBalance()),
                "Available for transfer", HabeshaTheme.GOLD_PRIMARY);
        balanceCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        balanceCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        panel.add(balanceCard);
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

    // ── Actions ───────────────────────────────────────────────────────────────

    private void handleTransfer() {
        if (recipientField.getText().trim().isEmpty())     { showError("Recipient account is required."); return; }
        if (recipientNameField.getText().trim().isEmpty()) { showError("Recipient name is required."); return; }
        if (amountField.getText().trim().isEmpty())        { showError("Amount is required."); return; }

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            showError("Invalid amount.");
            return;
        }

        boolean isInternal = bankCombo.getSelectedIndex() == 0;
        double  fee        = isInternal ? 0.00 : 25.00;
        String  recipient  = recipientField.getText().trim();
        String  name       = recipientNameField.getText().trim();
        String  desc       = descArea.getText().trim();

        String feeNote = fee > 0 ? String.format("\nTransfer fee: %.2f ETB\nTotal deducted: %.2f ETB", fee, amount + fee) : "";
        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Transfer %.2f ETB to %s?\nAccount: %s%s", amount, name, recipient, feeNote),
                "Confirm Transfer", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Transaction tx = txService.transfer(amount, recipient, name, desc, isInternal);

            JOptionPane.showMessageDialog(this,
                    String.format(
                            "✓  Transfer successful!\n\n" +
                                    "Amount:      %.2f ETB\n" +
                                    "Fee:         %.2f ETB\n" +
                                    "New Balance: %.2f ETB\n" +
                                    "Reference:   %s",
                            amount, fee,
                            UserSession.getInstance().getBalance(),
                            tx.getReferenceNumber()),
                    "Transfer Sent", JOptionPane.INFORMATION_MESSAGE);

            clearForm();
            mainFrame.refreshAllUI();   // sync dashboard + history

        } catch (BankingException ex) {
            showError(ex.getMessage());
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
