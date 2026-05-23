package com.habeshabank.ui.components;

import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A rounded card panel used to group content sections in forms and dashboards.
 */
public class SectionPanel extends JPanel {

    private final String sectionTitle;

    public SectionPanel(String sectionTitle) {
        this.sectionTitle = sectionTitle;
        setOpaque(false);
        setLayout(new BorderLayout());
    }

    public SectionPanel(String sectionTitle, LayoutManager layout) {
        this.sectionTitle = sectionTitle;
        setOpaque(false);
        setLayout(layout);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Card background
        RoundRectangle2D bg = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 10, 10);
        g2.setColor(HabeshaTheme.BLACK_CARD);
        g2.fill(bg);
        g2.setColor(HabeshaTheme.BLACK_BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(bg);

        g2.dispose();
    }

    @Override
    public Insets getInsets() {
        return new Insets(16, 20, 16, 20);
    }
}