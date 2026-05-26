package com.habeshabank.service;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Monitors user inactivity and triggers automatic logout after a configurable
 * timeout period.
 *
 * Mechanism
 * ─────────
 * A Swing {@link Timer} counts down from {@link #TIMEOUT_MS}.  Every time the
 * user moves the mouse, clicks, or presses a key inside the application window,
 * an AWT event listener calls {@link #resetTimer()} to restart the countdown.
 *
 * On timeout, the provided {@link Runnable logoutAction} is invoked on the EDT.
 * MainFrame passes {@code this::performLogout} so the session is cleared and
 * LoginFrame is shown — identical to clicking the "Logout" button.
 *
 * Lifecycle
 * ─────────
 * Call {@link #start()} when MainFrame becomes visible.
 * Call {@link #stop()}  when the user logs out (or before dispose()).
 *
 * Thread safety
 * ─────────────
 * All Swing Timer callbacks run on the EDT.
 * The AWT event listener is also delivered on the EDT.
 * No external synchronisation required.
 */
public class SessionTimeoutManager {

    /** 10 minutes in milliseconds. */
    public static final int TIMEOUT_MS = 10 * 60 * 1000;

    /** Warning shown 60 seconds before logout. */
    private static final int WARN_BEFORE_MS = 60 * 1000;

    private final Runnable       logoutAction;
    private final Timer          countdownTimer;
    private final AWTEventListener activityListener;

    private boolean warningShown = false;

    /**
     * @param logoutAction callback invoked on the EDT when the session times out
     */
    public SessionTimeoutManager(Runnable logoutAction) {
        this.logoutAction = logoutAction;

        // Main countdown timer — fires once after TIMEOUT_MS
        this.countdownTimer = new Timer(TIMEOUT_MS, e -> handleTimeout());
        this.countdownTimer.setRepeats(false);

        // AWT event listener — resets the timer on any mouse or keyboard activity
        this.activityListener = event -> {
            if (event instanceof MouseEvent || event instanceof KeyEvent) {
                resetTimer();
            }
        };
    }

    /** Starts monitoring. Call after MainFrame.setVisible(true). */
    public void start() {
        Toolkit.getDefaultToolkit().addAWTEventListener(
                activityListener,
                AWTEvent.MOUSE_EVENT_MASK
                        | AWTEvent.MOUSE_MOTION_EVENT_MASK
                        | AWTEvent.KEY_EVENT_MASK);

        countdownTimer.start();
        System.out.println("[SessionTimeout] Timer started. Timeout: "
                + (TIMEOUT_MS / 60_000) + " min.");
    }

    /** Stops monitoring and removes the AWT listener. Call before logout/dispose. */
    public void stop() {
        countdownTimer.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
        System.out.println("[SessionTimeout] Timer stopped.");
    }

    /**
     * Resets the countdown to the full timeout period.
     * Called automatically by the AWT listener on every user action.
     * Can also be called manually (e.g. after a successful transaction).
     */
    public void resetTimer() {
        warningShown = false;
        countdownTimer.restart();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void handleTimeout() {
        // Must be on EDT — Timer guarantees this
        stop();
        JOptionPane.showMessageDialog(
                null,
                "Your session has expired due to inactivity.\n"
                        + "You have been logged out for your security.",
                "Session Expired",
                JOptionPane.WARNING_MESSAGE);
        logoutAction.run();
    }
}
