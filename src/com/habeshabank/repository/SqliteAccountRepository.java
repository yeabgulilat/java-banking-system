package com.habeshabank.repository;

import com.habeshabank.database.DatabaseManager;
import com.habeshabank.model.Account;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * SQLite-backed implementation of {@link AccountRepository}.
 *
 * Mapping: Java fields ↔ SQL columns
 * ───────────────────────────────────
 *   Account.id                 → accounts.id                 (AUTOINCREMENT)
 *   Account.accountNumber      → accounts.account_number
 *   Account.userId             → accounts.user_id
 *   Account.accountType        → accounts.account_type       (enum name)
 *   Account.status             → accounts.status             (enum name)
 *   Account.balance            → accounts.balance            (REAL)
 *   Account.currency           → accounts.currency
 *   Account.openedAt           → accounts.opened_at          (ISO-8601 text)
 *   Account.lastTransactionAt  → accounts.last_transaction_at (ISO-8601 text, nullable)
 */
public class SqliteAccountRepository implements AccountRepository {

    // ── save ──────────────────────────────────────────────────────────────────

    @Override
    public Account save(Account account) {
        String sql = """
            INSERT INTO accounts
                (account_number, user_id, account_type, status, balance,
                 currency, opened_at, last_transaction_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, account.getAccountNumber());
            ps.setLong(2,   account.getUserId());
            ps.setString(3, account.getAccountType().name());
            ps.setString(4, account.getStatus().name());
            ps.setDouble(5, account.getBalance());
            ps.setString(6, account.getCurrency());
            ps.setString(7, account.getOpenedAt() != null
                    ? account.getOpenedAt().toString() : LocalDateTime.now().toString());
            ps.setString(8, account.getLastTransactionAt() != null
                    ? account.getLastTransactionAt().toString() : null);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) account.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to save account: " + account.getAccountNumber(), e);
        }

        return account;
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Override
    public void update(Account account) {
        String sql = """
            UPDATE accounts SET
                balance             = ?,
                status              = ?,
                last_transaction_at = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setDouble(1, account.getBalance());
            ps.setString(2, account.getStatus().name());
            ps.setString(3, account.getLastTransactionAt() != null
                    ? account.getLastTransactionAt().toString() : null);
            ps.setLong(4,   account.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update account id=" + account.getId(), e);
        }
    }

    // ── findByAccountNumber ───────────────────────────────────────────────────

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ? LIMIT 1";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find account by number: " + accountNumber, e);
        }
        return Optional.empty();
    }

    // ── findByUserId ──────────────────────────────────────────────────────────

    @Override
    public Optional<Account> findByUserId(long userId) {
        String sql = "SELECT * FROM accounts WHERE user_id = ? LIMIT 1";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find account for user id=" + userId, e);
        }
        return Optional.empty();
    }

    // ── existsByAccountNumber ─────────────────────────────────────────────────

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        String sql = "SELECT 1 FROM accounts WHERE account_number = ? LIMIT 1";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to check account number existence: " + accountNumber, e);
        }
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    private Account mapRow(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getLong("id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setUserId(rs.getLong("user_id"));
        a.setAccountType(Account.AccountType.valueOf(rs.getString("account_type")));
        a.setStatus(Account.AccountStatus.valueOf(rs.getString("status")));
        a.setBalance(rs.getDouble("balance"));
        a.setCurrency(rs.getString("currency"));

        String openedAt = rs.getString("opened_at");
        if (openedAt != null) a.setOpenedAt(LocalDateTime.parse(openedAt));

        String lastTx = rs.getString("last_transaction_at");
        if (lastTx != null) a.setLastTransactionAt(LocalDateTime.parse(lastTx));

        return a;
    }

    private Connection connection() {
        return DatabaseManager.getInstance().getConnection();
    }
}
