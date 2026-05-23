package com.habeshabank.ui.theme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;

public class HabeshaTheme {

    // ── Core Palette ──────────────────────────────────────────────────────────
    public static final Color BLACK_DEEP    = new Color(0x0A, 0x0A, 0x0A);
    public static final Color BLACK_RICH    = new Color(0x12, 0x10, 0x0E);
    public static final Color BLACK_CARD    = new Color(0x1A, 0x17, 0x12);
    public static final Color BLACK_PANEL   = new Color(0x22, 0x1E, 0x18);
    public static final Color BLACK_BORDER  = new Color(0x33, 0x2D, 0x22);

    public static final Color GOLD_PRIMARY  = new Color(0xC9, 0xA0, 0x2A);
    public static final Color GOLD_BRIGHT   = new Color(0xE8, 0xC8, 0x40);
    public static final Color GOLD_MUTED    = new Color(0x8B, 0x6F, 0x1E);
    public static final Color GOLD_ACCENT   = new Color(0xF5, 0xD7, 0x60);
    public static final Color GOLD_GLOW     = new Color(0xC9, 0xA0, 0x2A, 80);

    public static final Color CREAM_LIGHT   = new Color(0xF5, 0xF0, 0xE8);
    public static final Color CREAM_MID     = new Color(0xD4, 0xC9, 0xB0);
    public static final Color CREAM_DIM     = new Color(0x9A, 0x90, 0x7A);

    public static final Color GREEN_SUCCESS = new Color(0x2E, 0x7D, 0x4F);
    public static final Color RED_DANGER    = new Color(0x8B, 0x1A, 0x1A);
    public static final Color BLUE_INFO     = new Color(0x1A, 0x4A, 0x7A);

    // ── Typography ────────────────────────────────────────────────────────────
    public static final Font FONT_DISPLAY   = new Font("Palatino Linotype", Font.BOLD,  28);
    public static final Font FONT_TITLE     = new Font("Palatino Linotype", Font.BOLD,  20);
    public static final Font FONT_HEADING   = new Font("Palatino Linotype", Font.BOLD,  16);
    public static final Font FONT_SUBHEAD   = new Font("Palatino Linotype", Font.PLAIN, 13);
    public static final Font FONT_BODY      = new Font("Segoe UI",          Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI",          Font.BOLD,  13);
    public static final Font FONT_SMALL     = new Font("Segoe UI",          Font.PLAIN, 11);
    public static final Font FONT_MONO      = new Font("Consolas",           Font.PLAIN, 12);
    public static final Font FONT_AMOUNT    = new Font("Palatino Linotype", Font.BOLD,  22);

    // ── Borders ───────────────────────────────────────────────────────────────
    public static Border goldBorder() {
        return new LineBorder(GOLD_MUTED, 1);
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                new LineBorder(BLACK_BORDER, 1),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        );
    }

    public static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
                new LineBorder(GOLD_MUTED, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        );
    }

    // ── UIManager Global Overrides ─────────────────────────────────────────────
    public static void apply() {
        UIManager.put("Panel.background",              BLACK_RICH);
        UIManager.put("Frame.background",              BLACK_DEEP);

        UIManager.put("Label.foreground",              CREAM_LIGHT);
        UIManager.put("Label.font",                    FONT_BODY);

        UIManager.put("Button.background",             GOLD_PRIMARY);
        UIManager.put("Button.foreground",             BLACK_DEEP);
        UIManager.put("Button.font",                   FONT_BODY_BOLD);
        UIManager.put("Button.arc",                    6);
        UIManager.put("Button.focusedBorderColor",     GOLD_BRIGHT);
        UIManager.put("Button.hoverBackground",        GOLD_BRIGHT);
        UIManager.put("Button.pressedBackground",      GOLD_MUTED);

        UIManager.put("TextField.background",          BLACK_CARD);
        UIManager.put("TextField.foreground",          CREAM_LIGHT);
        UIManager.put("TextField.caretForeground",     GOLD_PRIMARY);
        UIManager.put("TextField.selectionBackground", GOLD_MUTED);
        UIManager.put("TextField.font",                FONT_BODY);

        UIManager.put("PasswordField.background",      BLACK_CARD);
        UIManager.put("PasswordField.foreground",      CREAM_LIGHT);
        UIManager.put("PasswordField.caretForeground", GOLD_PRIMARY);
        UIManager.put("PasswordField.font",            FONT_BODY);

        UIManager.put("ComboBox.background",           BLACK_CARD);
        UIManager.put("ComboBox.foreground",           CREAM_LIGHT);
        UIManager.put("ComboBox.font",                 FONT_BODY);

        UIManager.put("Table.background",              BLACK_CARD);
        UIManager.put("Table.foreground",              CREAM_MID);
        UIManager.put("Table.gridColor",               BLACK_BORDER);
        UIManager.put("Table.selectionBackground",     GOLD_GLOW);
        UIManager.put("Table.selectionForeground",     CREAM_LIGHT);
        UIManager.put("Table.font",                    FONT_BODY);
        UIManager.put("TableHeader.background",        BLACK_PANEL);
        UIManager.put("TableHeader.foreground",        GOLD_PRIMARY);
        UIManager.put("TableHeader.font",              FONT_BODY_BOLD);

        UIManager.put("ScrollBar.thumb",               BLACK_BORDER);
        UIManager.put("ScrollBar.track",               BLACK_CARD);
        UIManager.put("ScrollPane.background",         BLACK_CARD);

        UIManager.put("TabbedPane.background",         BLACK_RICH);
        UIManager.put("TabbedPane.foreground",         CREAM_MID);
        UIManager.put("TabbedPane.selectedBackground", BLACK_CARD);
        UIManager.put("TabbedPane.selectedForeground", GOLD_PRIMARY);

        UIManager.put("Separator.foreground",          BLACK_BORDER);

        UIManager.put("OptionPane.background",         BLACK_CARD);
        UIManager.put("OptionPane.messageForeground",  CREAM_LIGHT);
    }
}