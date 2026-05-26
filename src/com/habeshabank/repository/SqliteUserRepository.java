package com.habeshabank.repository;

import com.habeshabank.database.DatabaseManager;
import com.habeshabank.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * SQLite-backed implementation of {@link UserRepository}.
 *
 * Every method fetches the connection fresh from {@link DatabaseManager}
 * rather than caching it — this is correct for a single-threaded Swing app
 * and safe for WAL-mode SQLite.
 *
 * Mapping: Java fields ↔ SQL columns
 * ───────────────────────────────────
 *   User.id                → users.id             (AUTOINCREMENT)
 *   User.username          → users.username
 *   User.passwordHash      → users.password_hash
 *   User.fullName          → users.full_name
 *   User.email             → users.email
 *   User.phoneNumber       → users.phone_number
 *   User.dateOfBirth       → users.date_of_birth  (ISO-8601 text)
 *   User.nationalIdNumber  → users.national_id_number
 *   User.active            → users.active          (1/0)
 *   User.locked            → users.locked          (1/0)
 *   User.failedLoginCount  → users.failed_login_count
 *   User.createdAt         → users.created_at      (ISO-8601 text)
 *   User.lastLoginAt       → users.last_login_at   (ISO-8601 text, nullable)
 */
public class SqliteUserRepository implements UserRepository {

    // ── save ──────────────────────────────────────────────────────────────────

    @Override
    public User save(User user) {
        String sql = """
            INSERT INTO users
                (username, password_hash, pin_hash, full_name, email, phone_number,
                 date_of_birth, national_id_number, active, locked,
                 failed_login_count, created_at, last_login_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1,  user.getUsername());
            ps.setString(2,  user.getPasswordHash());
            ps.setString(3,  user.getPinHash());
            ps.setString(4,  user.getFullName());
            ps.setString(5,  user.getEmail());
            ps.setString(6,  user.getPhoneNumber());
            ps.setString(7,  user.getDateOfBirth() != null
                    ? user.getDateOfBirth().toString() : null);
            ps.setString(8,  user.getNationalIdNumber());
            ps.setInt(9,     user.isActive()  ? 1 : 0);
            ps.setInt(10,    user.isLocked()  ? 1 : 0);
            ps.setInt(11,    user.getFailedLoginCount());
            ps.setString(12, user.getCreatedAt() != null
                    ? user.getCreatedAt().toString() : LocalDateTime.now().toString());
            ps.setString(13, user.getLastLoginAt() != null
                    ? user.getLastLoginAt().toString() : null);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + user.getUsername(), e);
        }

        return user;
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Override
    public void update(User user) {
        String sql = """
            UPDATE users SET
                password_hash      = ?,
                pin_hash           = ?,
                full_name          = ?,
                email              = ?,
                phone_number       = ?,
                active             = ?,
                locked             = ?,
                failed_login_count = ?,
                last_login_at      = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, user.getPasswordHash());
            ps.setString(2, user.getPinHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhoneNumber());
            ps.setInt(6,    user.isActive() ? 1 : 0);
            ps.setInt(7,    user.isLocked() ? 1 : 0);
            ps.setInt(8,    user.getFailedLoginCount());
            ps.setString(9, user.getLastLoginAt() != null
                    ? user.getLastLoginAt().toString() : null);
            ps.setLong(10,  user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user id=" + user.getId(), e);
        }
    }

    // ── findByUsername ────────────────────────────────────────────────────────

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by username: " + username, e);
        }
        return Optional.empty();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by id=" + id, e);
        }
        return Optional.empty();
    }

    // ── existsByUsername ──────────────────────────────────────────────────────

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check username existence: " + username, e);
        }
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setPinHash(rs.getString("pin_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhoneNumber(rs.getString("phone_number"));

        String dob = rs.getString("date_of_birth");
        if (dob != null) u.setDateOfBirth(LocalDate.parse(dob));

        u.setNationalIdNumber(rs.getString("national_id_number"));
        u.setActive(rs.getInt("active")  == 1);
        u.setLocked(rs.getInt("locked")  == 1);
        u.setFailedLoginCount(rs.getInt("failed_login_count"));

        String createdAt = rs.getString("created_at");
        if (createdAt != null) u.setCreatedAt(LocalDateTime.parse(createdAt));

        String lastLogin = rs.getString("last_login_at");
        if (lastLogin != null) u.setLastLoginAt(LocalDateTime.parse(lastLogin));

        return u;
    }

    private Connection connection() {
        return DatabaseManager.getInstance().getConnection();
    }
}
