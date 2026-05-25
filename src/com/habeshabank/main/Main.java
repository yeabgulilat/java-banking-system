package com.habeshabank.main;

import com.formdev.flatlaf.FlatDarkLaf;
import com.habeshabank.database.DatabaseManager;
import com.habeshabank.database.DatabaseSeeder;
import com.habeshabank.ui.login.LoginFrame;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;
import java.sql.SQLException;

/**
 * Application entry point.
 *
 * Phase 4 startup sequence
 * ────────────────────────
 * 1. DatabaseManager.initialize() — opens SQLite connection, creates schema
 * 2. DatabaseSeeder.seed()        — inserts demo user if not already present
 * 3. FlatDarkLaf.setup()          — install UI look and feel
 * 4. HabeshaTheme.apply()         — apply Habesha colour/font overrides
 * 5. LoginFrame                   — show the login screen
 * 6. Shutdown hook                — close DB connection cleanly on JVM exit
 *
 * Database errors at step 1 are fatal — shown as a dialog and the app exits.
 * All other exceptions fall through to the existing generic error dialog.
 */
public class Main {

    public static void main(String[] args) {

        // ── Step 1 & 2: Database (runs on main thread before Swing) ──────────
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            db.initialize();
            new DatabaseSeeder(db.getConnection()).seed();
        } catch (SQLException e) {
            // Database failure is fatal — show dialog then exit
            JOptionPane.showMessageDialog(null,
                    "Failed to initialize database:\n" + e.getMessage()
                            + "\n\nPlease ensure the application has write access to:\n"
                            + System.getProperty("user.home") + "/HabeshaBankData/",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // ── Step 6: Shutdown hook — close DB on JVM exit ──────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Main] Shutdown hook: closing database...");
            DatabaseManager.getInstance().close();
        }, "db-shutdown"));

        // ── Steps 3–5: Swing UI (runs on EDT) ────────────────────────────────
        SwingUtilities.invokeLater(() -> {
            try {
                FlatDarkLaf.setup();
                HabeshaTheme.apply();

                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to initialize application: " + e.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
