package com.habeshabank.main;

import com.formdev.flatlaf.FlatDarkLaf;
import com.habeshabank.ui.login.LoginFrame;
import com.habeshabank.ui.theme.HabeshaTheme;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Install FlatLaf dark base
                FlatDarkLaf.setup();

                // Apply Habesha custom theme overrides
                HabeshaTheme.apply();

                // Launch login screen
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