package com.habeshabank.ui.components;

import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A custom-painted gold button with hover/press effects and optional icon support.
 */
public class GoldButton extends JButton {

    public enum Style { PRIMARY, OUTLINE, DANGER }

    private final Style style;
    private boolean hovered = false;
    private boolean pressed = false;

    public GoldButton(String text) {
        this(text, Style.PRIMARY);
    }

    public GoldButton(String text, Style style) {
        super(text);
        this.style = style;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(HabeshaTheme.FONT_BODY_BOLD);
        setPreferredSize(new Dimension(getPreferredSize().width, 40));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true;  repaint(); }
            @Override public void mouseReleased(MouseEvent e){ pressed = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = 6;
        RoundRectangle2D rect = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);

        switch (style) {
            case PRIMARY -> {
                Color base = pressed ? HabeshaTheme.GOLD_MUTED
                        : hovered ? HabeshaTheme.GOLD_BRIGHT
                          : HabeshaTheme.GOLD_PRIMARY;
                g2.setColor(base);
                g2.fill(rect);
                g2.setColor(HabeshaTheme.BLACK_DEEP);
            }
            case OUTLINE -> {
                if (hovered || pressed) {
                    g2.setColor(pressed ? HabeshaTheme.GOLD_GLOW : new Color(0xC9, 0xA0, 0x2A, 30));
                    g2.fill(rect);
                }
                g2.setColor(pressed ? HabeshaTheme.GOLD_MUTED : HabeshaTheme.GOLD_PRIMARY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(rect);
                g2.setColor(HabeshaTheme.GOLD_PRIMARY);
            }
            case DANGER -> {
                Color base = pressed ? new Color(0x6A, 0x10, 0x10)
                        : hovered ? new Color(0xA0, 0x20, 0x20)
                          : HabeshaTheme.RED_DANGER;
                g2.setColor(base);
                g2.fill(rect);
                g2.setColor(HabeshaTheme.CREAM_LIGHT);
            }
        }

        // Draw text
        FontMetrics fm = g2.getFontMetrics(getFont());
        g2.setFont(getFont());
        int tx = (w - fm.stringWidth(getText())) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), tx, ty);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width + 32, 120), 40);
    }
}