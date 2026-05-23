package com.habeshabank.ui.components;

import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Styled password field with gold focus ring and placeholder support.
 */
public class HabeshaPasswordField extends JPasswordField {

    private String placeholder;

    public HabeshaPasswordField(String placeholder, int columns) {
        super(columns);
        this.placeholder = placeholder;
        applyStyle();
    }

    private void applyStyle() {
        setFont(HabeshaTheme.FONT_BODY);
        setForeground(HabeshaTheme.CREAM_LIGHT);
        setBackground(HabeshaTheme.BLACK_CARD);
        setCaretColor(HabeshaTheme.GOLD_PRIMARY);
        setSelectionColor(HabeshaTheme.GOLD_GLOW);
        setSelectedTextColor(HabeshaTheme.CREAM_LIGHT);
        setPreferredSize(new Dimension(getPreferredSize().width, 40));
        updateBorder(false);

        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { updateBorder(true);  repaint(); }
            @Override public void focusLost(FocusEvent e)   { updateBorder(false); repaint(); }
        });
    }

    private void updateBorder(boolean focused) {
        Color borderColor = focused ? HabeshaTheme.GOLD_PRIMARY : HabeshaTheme.BLACK_BORDER;
        setBorder(new CompoundBorder(
                new LineBorder(borderColor, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (placeholder != null && getPassword().length == 0 && !isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setFont(getFont().deriveFont(Font.ITALIC));
            g2.setColor(HabeshaTheme.CREAM_DIM);
            Insets insets = getInsets();
            FontMetrics fm = g2.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(placeholder, insets.left, y);
            g2.dispose();
        }
    }
}