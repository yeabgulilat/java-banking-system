package com.habeshabank.ui.components;

import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A dashboard stat card showing a label, a large value, and a subtitle/trend.
 */
public class StatCard extends JPanel {

    private final String label;
    private String value;
    private String subtitle;
    private final Color accentColor;

    public StatCard(String label, String value, String subtitle, Color accentColor) {
        this.label       = label;
        this.value       = value;
        this.subtitle    = subtitle;
        this.accentColor = accentColor;

        setOpaque(false);
        setPreferredSize(new Dimension(220, 120));
    }

    public void setValue(String value) {
        this.value = value;
        repaint();
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Card background
        RoundRectangle2D bg = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 10, 10);
        g2.setColor(HabeshaTheme.BLACK_CARD);
        g2.fill(bg);

        // Border
        g2.setColor(HabeshaTheme.BLACK_BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(bg);

        // Left accent bar
        RoundRectangle2D bar = new RoundRectangle2D.Float(0, 0, 4, h - 1, 4, 4);
        g2.setColor(accentColor);
        g2.fill(bar);

        int pad = 18;

        // Label
        g2.setFont(HabeshaTheme.FONT_SMALL);
        g2.setColor(HabeshaTheme.CREAM_DIM);
        g2.drawString(label, pad, 24);

        // Value
        g2.setFont(HabeshaTheme.FONT_AMOUNT);
        g2.setColor(HabeshaTheme.CREAM_LIGHT);
        g2.drawString(value, pad, 62);

        // Subtitle / trend
        g2.setFont(HabeshaTheme.FONT_SMALL);
        g2.setColor(accentColor);
        g2.drawString(subtitle, pad, 88);

        // Decorative corner dot
        g2.setColor(accentColor);
        g2.fillOval(w - 20, 12, 8, 8);

        g2.dispose();
    }
}