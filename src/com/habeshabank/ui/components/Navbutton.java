package com.habeshabank.ui.components;

import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A sidebar navigation item that shows an icon character, label, and active/hover states.
 */
public class NavButton extends JPanel {

    private final String iconText;   // Unicode symbol acting as icon
    private final String labelText;
    private boolean active  = false;
    private boolean hovered = false;
    private Runnable onClick;

    public NavButton(String iconText, String labelText) {
        this.iconText  = iconText;
        this.labelText = labelText;

        setOpaque(false);
        setPreferredSize(new Dimension(220, 48));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            @Override public void mouseClicked(MouseEvent e) { if (onClick != null) onClick.run(); }
        });
    }

    public void setOnClick(Runnable r) { this.onClick = r; }

    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    public boolean isActive() { return active; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        if (active) {
            // Active: gold background strip with left accent
            RoundRectangle2D bg = new RoundRectangle2D.Float(8, 2, w - 16, h - 4, 8, 8);
            g2.setColor(new Color(0xC9, 0xA0, 0x2A, 25));
            g2.fill(bg);

            // Left indicator bar
            g2.setColor(HabeshaTheme.GOLD_PRIMARY);
            g2.fillRoundRect(0, 8, 3, h - 16, 2, 2);

        } else if (hovered) {
            RoundRectangle2D bg = new RoundRectangle2D.Float(8, 2, w - 16, h - 4, 8, 8);
            g2.setColor(new Color(0xFF, 0xFF, 0xFF, 8));
            g2.fill(bg);
        }

        // Icon
        g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
        g2.setColor(active ? HabeshaTheme.GOLD_PRIMARY : HabeshaTheme.CREAM_DIM);
        g2.drawString(iconText, 20, h / 2 + 6);

        // Label
        g2.setFont(active ? HabeshaTheme.FONT_BODY_BOLD : HabeshaTheme.FONT_BODY);
        g2.setColor(active ? HabeshaTheme.CREAM_LIGHT : HabeshaTheme.CREAM_DIM);
        g2.drawString(labelText, 50, h / 2 + 5);

        g2.dispose();
    }
}