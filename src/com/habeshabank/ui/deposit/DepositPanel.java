package com.habeshabank.ui.deposit;

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
 * Deposit funds panel.
 * Phase 2: handleDeposit() delegates to TransactionService.
 * Phase 3: implements Refreshable — refreshData() updates the balance StatCard
 *          so it always shows live balance after every transaction.
 *          All layout and styling unchanged from Phase 1/2.
 */
public class DepositPanel extends JPanel implements Refreshable {

    private final MainFrame mainFrame;
    private HabeshaTextField amountField;
    private JComboBox<String> sourceCombo;
    private JTextArea notesArea;
    private JLabel balanceLabel;

    // Phase 3: promoted to field so refreshData() can update it
    private StatCard balanceCard;

    private final TransactionService txService = TransactionService.getInstance();

    public DepositPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(true);
        setBackground(HabeshaTheme.BLACK_DEEP);
        setLayout(new BorderLayout());
        add(buildScrollableContent(), BorderLayout.CENTER);
    }

    // ── Refreshable ───────────────────────────────────────────────────────────

    /** Updates the balance StatCard to show the live session balance. */
    @Override
    public void refreshData() {
        if (balanceCard != null) {
            balanceCard.setValue(formatBalance(UserSession.getInstance().getBalance()));
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
        columns.add(buildDepositForm());
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

        JLabel title = new JLabel("Deposit Funds");
        title.setFont(HabeshaTheme.FONT_DISPLAY);
        title.setForeground(HabeshaTheme.CREAM_LIGHT);

        JLabel sub = new JLabel("Add money to your Habesha Bank account");
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

    private JPanel buildDepositForm() {
        JPanel card = new SectionPanel("Deposit Details", null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel formTitle = new JLabel("Deposit Details");
        formTitle.setFont(HabeshaTheme.FONT_HEADING);
        formTitle.setForeground(HabeshaTheme.GOLD_PRIMARY);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(formTitle);
        card.add(Box.createVerticalStrut(20));

        card.add(fieldLabel("Amount (ETB)"));
        card.add(Box.createVerticalStrut(6));
        amountField = new HabeshaTextField("e.g. 5000.00", 20);
        amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        amountField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(amountField);
        card.add(Box.createVerticalStrut(20));

        card.add(fieldLabel("Source / Method"));
        card.add(Box.createVerticalStrut(6));
        String[] sources = { "Cash Deposit", "Bank Transfer", "Mobile Money", "Cheque" };
        sourceCombo = new JComboBox<>(sources);
        sourceCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        sourceCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sourceCombo.setFont(HabeshaTheme.FONT_BODY);
        card.add(sourceCombo);
        card.add(Box.createVerticalStrut(20));

        card.add(fieldLabel("Notes (optional)"));
        card.add(Box.createVerticalStrut(6));
        notesArea = new JTextArea(3, 20);
        notesArea.setFont(HabeshaTheme.FONT_BODY);
        notesArea.setBackground(HabeshaTheme.BLACK_CARD);
        notesArea.setForeground(HabeshaTheme.CREAM_LIGHT);
        notesArea.setCaretColor(HabeshaTheme.GOLD_PRIMARY);
        notesArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HabeshaTheme.BLACK_BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        notesArea.setLineWrap(true);
        JScrollPane noteScroll = new JScrollPane(notesArea);
        noteScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        noteScroll.setBorder(BorderFactory.createEmptyBorder());
        noteScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(noteScroll);
        card.add(Box.createVerticalStrut(28));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        GoldButton submitBtn = new GoldButton("Confirm Deposit");
        submitBtn.setPreferredSize(new Dimension(180, 42));
        submitBtn.addActionListener(e -> handleDeposit());
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

        // Balance card — assigned to field so refreshData() can update it
        balanceCard = new StatCard("Current Balance",
                formatBalance(UserSession.getInstance().getBalance()),
                "Available funds", HabeshaTheme.GOLD_PRIMARY);
        balanceCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        balanceCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        panel.add(balanceCard);
        panel.add(Box.createVerticalStrut(16));

        JPanel info = new SectionPanel("", null);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel infoTitle = new JLabel("Deposit Information");
        infoTitle.setFont(HabeshaTheme.FONT_HEADING);
        infoTitle.setForeground(HabeshaTheme.GOLD_PRIMARY);
        infoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(infoTitle);
        info.add(Box.createVerticalStrut(12));

        String[] bullets = {
                "• Minimum deposit: 100 ETB",
                "• Cash deposits are instant",
                "• Transfers may take 1–2 business days",
                "• A PDF receipt will be generated",
                "• Large deposits may require ID verification"
        };
        for (String b : bullets) {
            JLabel lbl = new JLabel(b);
            lbl.setFont(HabeshaTheme.FONT_BODY);
            lbl.setForeground(HabeshaTheme.CREAM_MID);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            info.add(lbl);
            info.add(Box.createVerticalStrut(6));
        }

        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(info);
        return panel;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Phase 2: delegates to TransactionService instead of mutating UserSession directly.
     * The success message now shows the reference number from the recorded Transaction.
     */
    private void handleDeposit() {
        String amtText = amountField.getText().trim();
        if (amtText.isEmpty()) {
            showError("Please enter an amount.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtText.replace(",", ""));
        } catch (NumberFormatException ex) {
            showError("Invalid amount. Please enter a number.");
            return;
        }

        String source = (String) sourceCombo.getSelectedItem();
        String notes  = notesArea.getText().trim();
        String desc   = source + (notes.isEmpty() ? "" : " – " + notes);

        try {
            Transaction tx = txService.deposit(amount, desc);

            JOptionPane.showMessageDialog(this,
                    String.format(
                            "✓  Deposit successful!\n\n" +
                                    "Amount:      %.2f ETB\n" +
                                    "New Balance: %.2f ETB\n" +
                                    "Reference:   %s",
                            amount,
                            UserSession.getInstance().getBalance(),
                            tx.getReferenceNumber()),
                    "Deposit Successful", JOptionPane.INFORMATION_MESSAGE);

            clearForm();
            mainFrame.refreshAllUI();
            mainFrame.resetSessionTimeout();

        } catch (BankingException ex) {
            showError(ex.getMessage());
        }
    }

    private void clearForm() {
        amountField.setText("");
        sourceCombo.setSelectedIndex(0);
        notesArea.setText("");
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Input Error", JOptionPane.WARNING_MESSAGE);
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(HabeshaTheme.FONT_SMALL);
        lbl.setForeground(HabeshaTheme.CREAM_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private String formatBalance(double v) {
        return String.format("%.2f ETB", v);
    }
}
