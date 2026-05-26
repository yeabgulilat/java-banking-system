package com.habeshabank.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;

/**
 * Central database lifecycle manager for Habesha Digital Banking System.
 *
 * Responsibilities
 * ────────────────
 * • Owns the single SQLite {@link Connection} for the entire application.
 * • Creates the database file in a platform-appropriate location on first run.
 * • Runs {@link #initializeSchema()} to CREATE TABLE IF NOT EXISTS on every
 *   startup — safe to call repeatedly; existing data is never touched.
 * • Provides {@link #getConnection()} to all repositories.
 * • {@link #close()} is called by Main on JVM shutdown via a shutdown hook.
 *
 * Database location
 * ─────────────────
 * The database file is stored at:
 *   {user.home}/HabeshaBankData/habesha_bank.db
 *
 * This survives IDE restarts, version changes, and project cleans.
 * The directory is created automatically if it does not exist.
 */
public class DatabaseManager {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String DB_DIR_NAME  = "HabeshaBankData";
    private static final String DB_FILE_NAME = "habesha_bank.db";

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Opens the SQLite connection and initialises the schema.
     * Must be called once at application startup, before any repository is used.
     *
     * @throws SQLException if the driver is missing or the file cannot be created
     */
    public synchronized void initialize() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return; // already open
        }

        String dbPath = resolveDatabasePath();
        System.out.println("[DB] Opening database at: " + dbPath);

        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

        // Performance pragmas — applied once per connection
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode = WAL");   // write-ahead logging
            st.execute("PRAGMA foreign_keys = ON");    // enforce FK constraints
            st.execute("PRAGMA synchronous = NORMAL"); // balanced durability/speed
        }

        initializeSchema();
        System.out.println("[DB] Schema ready.");
    }

    /**
     * Returns the live connection.
     * Repositories call this on every operation — they must NOT cache it.
     *
     * @throws IllegalStateException if {@link #initialize()} has not been called
     */
    public synchronized Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException(
                    "DatabaseManager.initialize() must be called before getConnection().");
        }
        return connection;
    }

    /** Closes the connection cleanly. Called by the JVM shutdown hook in Main. */
    public synchronized void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("[DB] Connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("[DB] Error closing connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    /**
     * Creates all tables if they do not already exist.
     * Safe to run on every startup — uses IF NOT EXISTS throughout.
     * Column order mirrors the domain model fields for easy mapping.
     */
    private void initializeSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {

            // ── users ──────────────────────────────────────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    username            TEXT    NOT NULL UNIQUE,
                    password_hash       TEXT    NOT NULL,
                    pin_hash            TEXT,
                    full_name           TEXT    NOT NULL,
                    email               TEXT    NOT NULL,
                    phone_number        TEXT,
                    date_of_birth       TEXT,
                    national_id_number  TEXT,
                    active              INTEGER NOT NULL DEFAULT 1,
                    locked              INTEGER NOT NULL DEFAULT 0,
                    failed_login_count  INTEGER NOT NULL DEFAULT 0,
                    created_at          TEXT    NOT NULL,
                    last_login_at       TEXT
                )
            """);

            // ── Migration: add pin_hash to existing databases (Phase 6) ───────
            // ALTER TABLE IGNORE is not valid SQL; we catch the error silently.
            try {
                st.execute("ALTER TABLE users ADD COLUMN pin_hash TEXT");
                System.out.println("[DB] Migration: added pin_hash column to users.");
            } catch (SQLException ignored) {
                // Column already exists — safe to ignore
            }

            // ── accounts ───────────────────────────────────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
                    account_number       TEXT    NOT NULL UNIQUE,
                    user_id              INTEGER NOT NULL,
                    account_type         TEXT    NOT NULL,
                    status               TEXT    NOT NULL DEFAULT 'ACTIVE',
                    balance              REAL    NOT NULL DEFAULT 0.0,
                    currency             TEXT    NOT NULL DEFAULT 'ETB',
                    opened_at            TEXT    NOT NULL,
                    last_transaction_at  TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);

            // ── transactions ───────────────────────────────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
                    reference_number     TEXT    NOT NULL UNIQUE,
                    account_number       TEXT    NOT NULL,
                    type                 TEXT    NOT NULL,
                    amount               REAL    NOT NULL,
                    balance_after        REAL    NOT NULL,
                    description          TEXT,
                    counterparty_account TEXT,
                    timestamp            TEXT    NOT NULL,
                    FOREIGN KEY (account_number) REFERENCES accounts(account_number)
                )
            """);

            // ── indexes for common queries ─────────────────────────────────────
            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_transactions_account_number
                ON transactions(account_number)
            """);
            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_transactions_timestamp
                ON transactions(timestamp)
            """);
            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_accounts_user_id
                ON accounts(user_id)
            """);
        }
    }

    // ── Path Resolution ───────────────────────────────────────────────────────

    private String resolveDatabasePath() {
        String home   = System.getProperty("user.home");
        File   dbDir  = new File(home, DB_DIR_NAME);

        if (!dbDir.exists() && !dbDir.mkdirs()) {
            throw new RuntimeException(
                    "Cannot create database directory: " + dbDir.getAbsolutePath());
        }

        return new File(dbDir, DB_FILE_NAME).getAbsolutePath();
    }
}
