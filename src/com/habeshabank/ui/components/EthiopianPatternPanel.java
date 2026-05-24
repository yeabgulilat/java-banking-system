package com.habeshabank.ui.components;

import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Renders a decorative Ethiopian-inspired geometric border/pattern strip.
 * Can be used as a top banner accent or sidebar decoration.
 */
public class EthiopianPatternPanel extends JPanel {

    public enum Orientation { HORIZONTAL, VERTICAL }

    private final Orientation orientation;
    private final int thickness;

    public EthiopianPatternPanel(Orientation orientation, int thickness) {
        this.orientation = orientation;
        this.thickness = thickness;
        setOpaque(true);

        if (orientation == Orientation.HORIZONTAL) {
            setPreferredSize(new Dimension(Integer.MAX_VALUE, thickness));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, thickness));
            setMinimumSize(new Dimension(0, thickness));
        } else {
            setPreferredSize(new Dimension(thickness, Integer.MAX_VALUE));
            setMaximumSize(new Dimension(thickness, Integer.MAX_VALUE));
            setMinimumSize(new Dimension(thickness, 0));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background fill
        g2.setColor(HabeshaTheme.BLACK_PANEL);
        g2.fillRect(0, 0, w, h);

        if (orientation == Orientation.HORIZONTAL) {
            paintHorizontalPattern(g2, w, h);
        } else {
            paintVerticalPattern(g2, w, h);
        }

        g2.dispose();
    }

    private void paintHorizontalPattern(Graphics2D g2, int w, int h) {
        int cellSize = h;
        int numCells = w / cellSize + 2;

        for (int i = 0; i < numCells; i++) {
            int x = i * cellSize;
            paintCell(g2, x, 0, cellSize, cellSize, i);
        }

        // Gold lines top and bottom
        g2.setColor(HabeshaTheme.GOLD_MUTED);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(0, 0, w, 0);
        g2.drawLine(0, h - 1, w, h - 1);
    }

    private void paintVerticalPattern(Graphics2D g2, int w, int h) {
        int cellSize = w;
        int numCells = h / cellSize + 2;

        for (int i = 0; i < numCells; i++) {
            int y = i * cellSize;
            paintCell(g2, 0, y, cellSize, cellSize, i);
        }

        // Gold lines left and right
        g2.setColor(HabeshaTheme.GOLD_MUTED);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(0, 0, 0, h);
        g2.drawLine(w - 1, 0, w - 1, h);
    }

    /** Paints one repeating pattern cell — alternates between two motifs. */
    private void paintCell(Graphics2D g2, int x, int y, int w, int h, int index) {
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        float r  = Math.min(w, h) * 0.38f;

        if (index % 2 == 0) {
            // Motif A: nested diamonds
            drawDiamond(g2, cx, cy, r, HabeshaTheme.GOLD_MUTED);
            drawDiamond(g2, cx, cy, r * 0.6f, HabeshaTheme.GOLD_PRIMARY);
            drawDiamond(g2, cx, cy, r * 0.25f, HabeshaTheme.GOLD_BRIGHT);
        } else {
            // Motif B: star-of-Solomon style cross
            drawCross(g2, cx, cy, r, HabeshaTheme.GOLD_MUTED);
            drawSmallSquare(g2, cx, cy, r * 0.3f, HabeshaTheme.GOLD_PRIMARY);
        }
    }

    private void drawDiamond(Graphics2D g2, float cx, float cy, float r, Color c) {
        int[] xs = { (int) cx, (int)(cx + r), (int) cx, (int)(cx - r) };
        int[] ys = { (int)(cy - r), (int) cy, (int)(cy + r), (int) cy };
        g2.setColor(c);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawPolygon(xs, ys, 4);
    }

    private void drawCross(Graphics2D g2, float cx, float cy, float r, Color c) {
        g2.setColor(c);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Line2D.Float(cx - r, cy, cx + r, cy));
        g2.draw(new Line2D.Float(cx, cy - r, cx, cy + r));
        // Diagonal arms (thinner)
        g2.setStroke(new BasicStroke(0.8f));
        float d = r * 0.7f;
        g2.draw(new Line2D.Float(cx - d, cy - d, cx + d, cy + d));
        g2.draw(new Line2D.Float(cx + d, cy - d, cx - d, cy + d));
    }

    private void drawSmallSquare(Graphics2D g2, float cx, float cy, float r, Color c) {
        g2.setColor(c);
        g2.fill(new Rectangle2D.Float(cx - r / 2, cy - r / 2, r, r));
    }
}