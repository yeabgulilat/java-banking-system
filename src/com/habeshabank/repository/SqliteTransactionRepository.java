package com.habeshabank.repository;

import com.habeshabank.database.DatabaseManager;
import com.habeshabank.model.Transaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite-backed implementation of {@link TransactionRepository}.
 *
 * Mapping: Java fields ↔ SQL columns
 * ───────────────────────────────────
 *   Transaction.id                  → transactions.id                (AUTOINCREMENT)
 *   Transaction.referenceNumber     → transactions.reference_number
 *   Transaction.type (enum name)    → transactions.type
 *   Transaction.amount              → transactions.amount
 *   Transaction.balanceAfter        → transactions.balance_after
 *   Transaction.description         → transactions.description
 *   Transaction.counterpartyAccount → transactions.counterparty_account (nullable)
 *   Transaction.timestamp           → transactions.timestamp            (ISO-8601 text)
 *
 * The account_number column links each transaction to an account for filtering.
 * It comes from TransactionService which passes it at record time.
 */
public class SqliteTransactionRepository implements TransactionRepository {

    // ── save ──────────────────────────────────────────────────────────────────

    @Override
    public Transaction save(Transaction tx) {
        String sql = """
            INSERT INTO transactions
                (reference_number, account_number, type, amount, balance_after,
                 description, counterparty_account, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, tx.getReferenceNumber());
            // account_number is stored in counterpartyAccount field when null — handled below
            // We need the account_number from TransactionService; stored in description context.
            // TransactionService passes it as a separate parameter — see save(tx, accountNumber).
            // For the interface-compatible overload we read it from referenceNumber context.
            // IMPORTANT: callers should use save(Transaction, String) overload.
            ps.setString(2, "UNKNOWN"); // placeholder; overload below is the real path
            ps.setString(3, tx.getType().name());
            ps.setDouble(4, tx.getAmount());
            ps.setDouble(5, tx.getBalanceAfter());
            ps.setString(6, tx.getDescription());
            ps.setString(7, tx.getCounterpartyAccount());
            ps.setString(8, tx.getTimestamp() != null
                    ? tx.getTimestamp().toString()
                    : LocalDateTime.now().toString());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) tx.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to save transaction ref=" + tx.getReferenceNumber(), e);
        }

        return tx;
    }

    /**
     * Preferred overload — also receives the owning account number.
     * TransactionService calls this version.
     */
    public Transaction save(Transaction tx, String accountNumber) {
        String sql = """
            INSERT INTO transactions
                (reference_number, account_number, type, amount, balance_after,
                 description, counterparty_account, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, tx.getReferenceNumber());
            ps.setString(2, accountNumber);
            ps.setString(3, tx.getType().name());
            ps.setDouble(4, tx.getAmount());
            ps.setDouble(5, tx.getBalanceAfter());
            ps.setString(6, tx.getDescription());
            ps.setString(7, tx.getCounterpartyAccount());
            ps.setString(8, tx.getTimestamp() != null
                    ? tx.getTimestamp().toString()
                    : LocalDateTime.now().toString());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) tx.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to save transaction ref=" + tx.getReferenceNumber(), e);
        }

        return tx;
    }

    // ── findByAccountNumber ───────────────────────────────────────────────────

    @Override
    public List<Transaction> findByAccountNumber(String accountNumber) {
        String sql = """
            SELECT * FROM transactions
            WHERE account_number = ?
            ORDER BY timestamp DESC
        """;

        List<Transaction> results = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to load transactions for account: " + accountNumber, e);
        }
        return results;
    }

    // ── findRecentByAccountNumber ─────────────────────────────────────────────

    @Override
    public List<Transaction> findRecentByAccountNumber(String accountNumber, int limit) {
        String sql = """
            SELECT * FROM transactions
            WHERE account_number = ?
            ORDER BY timestamp DESC
            LIMIT ?
        """;

        List<Transaction> results = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to load recent transactions for account: " + accountNumber, e);
        }
        return results;
    }

    // ── sumAmountByTypeAndMonth ───────────────────────────────────────────────

    @Override
    public double sumAmountByTypeAndMonth(String accountNumber,
                                          String type,
                                          YearMonth month) {
        // SQLite stores timestamps as ISO-8601 text; SUBSTR extracts YYYY-MM prefix
        String yearMonth = String.format("%04d-%02d", month.getYear(), month.getMonthValue());

        String sql = """
            SELECT COALESCE(SUM(amount), 0.0) AS total
            FROM transactions
            WHERE account_number = ?
              AND type = ?
              AND SUBSTR(timestamp, 1, 7) = ?
        """;

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, type);
            ps.setString(3, yearMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to sum transactions type=" + type + " month=" + month, e);
        }
        return 0.0;
    }

    // ── countByType ───────────────────────────────────────────────────────────

    @Override
    public long countByType(String accountNumber, String type) {
        String sql = """
            SELECT COUNT(*) AS cnt FROM transactions
            WHERE account_number = ? AND type = ?
        """;

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to count transactions type=" + type, e);
        }
        return 0L;
    }

    // ── sumAllByType ──────────────────────────────────────────────────────────

    @Override
    public double sumAllByType(String accountNumber, String type) {
        String sql = """
            SELECT COALESCE(SUM(amount), 0.0) AS total
            FROM transactions
            WHERE account_number = ? AND type = ?
        """;

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum all transactions type=" + type, e);
        }
        return 0.0;
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction();
        tx.setId(rs.getLong("id"));
        tx.setReferenceNumber(rs.getString("reference_number"));
        tx.setType(Transaction.Type.valueOf(rs.getString("type")));
        tx.setAmount(rs.getDouble("amount"));
        tx.setBalanceAfter(rs.getDouble("balance_after"));
        tx.setDescription(rs.getString("description"));
        tx.setCounterpartyAccount(rs.getString("counterparty_account"));

        String ts = rs.getString("timestamp");
        if (ts != null) tx.setTimestamp(LocalDateTime.parse(ts));

        return tx;
    }

    private Connection connection() {
        return DatabaseManager.getInstance().getConnection();
    }
}
