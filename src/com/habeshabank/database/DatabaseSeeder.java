package com.habeshabank.database;

import com.habeshabank.util.AccountNumberGenerator;
import com.habeshabank.util.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Seeds the database with the demo user and account on first run.
 *
 * Rules
 * ─────
 * • Completely idempotent — safe to call on every startup.
 *   Uses INSERT OR IGNORE so existing rows are never overwritten.
 * • Seeds the same credentials as the Phase 1–3 in-memory demo:
 *     Username / Account : tigist.alemu  (or the generated account number)
 *     Password           : demo1234
 *     Opening balance    : 47,850.00 ETB
 *     Account type       : PREMIUM_SAVINGS
 *
 * • After seeding, calls {@link AccountNumberGenerator#seed(long)} with the
 *   highest account sequence found in the database so new account numbers
 *   never collide with existing ones.
 */
public class DatabaseSeeder {

    private static final String DEMO_USERNAME = "tigist.alemu";
    private static final String DEMO_PASSWORD = "demo1234";
    private static final String DEMO_FULLNAME = "Tigist Alemu";
    private static final String DEMO_EMAIL    = "tigist.alemu@habeshabank.et";
    private static final String DEMO_PHONE    = "+251911000001";
    private static final double DEMO_BALANCE  = 47_850.00;

    private final Connection conn;

    public DatabaseSeeder(Connection conn) {
        this.conn = conn;
    }

    /**
     * Entry point — called once by {@link com.habeshabank.main.Main} after
     * {@link DatabaseManager#initialize()}.
     */
    public void seed() throws SQLException {
        seedDemoUser();
        syncAccountNumberGenerator();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void seedDemoUser() throws SQLException {
        // Skip entirely if demo user already exists
        if (userExists(DEMO_USERNAME)) {
            System.out.println("[Seeder] Demo user already exists — skipping seed.");
            return;
        }

        System.out.println("[Seeder] Seeding demo user...");

        String passwordHash = PasswordUtil.hash(DEMO_PASSWORD);
        String now          = LocalDateTime.now().toString();

        // ── Insert user ───────────────────────────────────────────────────────
        String insertUser = """
            INSERT OR IGNORE INTO users
                (username, password_hash, full_name, email, phone_number,
                 active, locked, failed_login_count, created_at)
            VALUES (?, ?, ?, ?, ?, 1, 0, 0, ?)
        """;

        long userId;
        try (PreparedStatement ps = conn.prepareStatement(
                insertUser, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, DEMO_USERNAME);
            ps.setString(2, passwordHash);
            ps.setString(3, DEMO_FULLNAME);
            ps.setString(4, DEMO_EMAIL);
            ps.setString(5, DEMO_PHONE);
            ps.setString(6, now);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("User insert returned no key.");
                userId = keys.getLong(1);
            }
        }

        // ── Generate account number ───────────────────────────────────────────
        // Seed generator to 141 so next() returns ETH-{year}-00142
        // matching the account number used across all existing UI demo data
        AccountNumberGenerator.seed(141L);
        String accountNumber = AccountNumberGenerator.next();

        // ── Insert account ────────────────────────────────────────────────────
        String insertAccount = """
            INSERT OR IGNORE INTO accounts
                (account_number, user_id, account_type, status, balance,
                 currency, opened_at)
            VALUES (?, ?, 'PREMIUM_SAVINGS', 'ACTIVE', ?, 'ETB', ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(insertAccount)) {
            ps.setString(1, accountNumber);
            ps.setLong(2, userId);
            ps.setDouble(3, DEMO_BALANCE);
            ps.setString(4, now);
            ps.executeUpdate();
        }

        System.out.println("[Seeder] Demo user seeded. Account: " + accountNumber);
    }

    /** Seeds the AccountNumberGenerator so new numbers never collide. */
    private void syncAccountNumberGenerator() throws SQLException {
        String sql = "SELECT account_number FROM accounts ORDER BY id DESC LIMIT 1";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String last = rs.getString("account_number");
                // Format: ETH-YYYY-NNNNN — extract the sequence part
                String[] parts = last.split("-");
                if (parts.length == 3) {
                    try {
                        long seq = Long.parseLong(parts[2]);
                        AccountNumberGenerator.seed(seq);
                        System.out.println("[Seeder] AccountNumberGenerator seeded to: " + seq);
                    } catch (NumberFormatException ignored) { /* non-standard number, leave as-is */ }
                }
            }
        }
    }

    private boolean userExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
