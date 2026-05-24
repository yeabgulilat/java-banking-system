package com.habeshabank.model;

import java.time.LocalDateTime;

/**
 * Domain entity representing a bank account.
 *
 * An Account belongs to exactly one User (via userId) and holds the
 * authoritative balance.  All balance mutations happen through
 * TransactionService — never by setting the balance field directly from UI.
 *
 * Designed to be persisted by an AccountRepository.  No UI or database
 * dependencies live here.
 */
public class Account {

    // ── Account Types ─────────────────────────────────────────────────────────

    public enum AccountType {
        SAVINGS         ("Savings Account"),
        CURRENT         ("Current Account"),
        PREMIUM_SAVINGS ("Premium Savings"),
        FIXED_DEPOSIT   ("Fixed Deposit");

        private final String displayName;
        AccountType(String displayName) { this.displayName = displayName; }
        public String getDisplayName()  { return displayName; }
    }

    // ── Status ────────────────────────────────────────────────────────────────

    public enum AccountStatus { ACTIVE, FROZEN, CLOSED }

    // ── Constants ─────────────────────────────────────────────────────────────

    public static final double MIN_DEPOSIT    = 100.00;
    public static final double MIN_WITHDRAWAL = 200.00;
    public static final double DAILY_ATM_LIMIT         = 10_000.00;
    public static final double DAILY_TRANSFER_LIMIT    = 50_000.00;
    public static final double COUNTER_WITHDRAWAL_MAX  = 100_000.00;
    public static final double INTERNAL_TRANSFER_FEE   = 0.00;
    public static final double EXTERNAL_TRANSFER_FEE   = 25.00;

    // ── Fields ────────────────────────────────────────────────────────────────

    private long          id;
    private String        accountNumber;   // formatted, e.g. "ETH-2024-00142"
    private long          userId;          // FK → User.id
    private AccountType   accountType;
    private AccountStatus status;
    private double        balance;
    private String        currency = "ETB";
    private LocalDateTime openedAt;
    private LocalDateTime lastTransactionAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Account() {
        this.status   = AccountStatus.ACTIVE;
        this.openedAt = LocalDateTime.now();
    }

    public Account(long userId, String accountNumber, AccountType accountType, double openingBalance) {
        this();
        this.userId        = userId;
        this.accountNumber = accountNumber;
        this.accountType   = accountType;
        this.balance       = openingBalance;
    }

    // ── Business Rules ────────────────────────────────────────────────────────

    public boolean isActive()      { return status == AccountStatus.ACTIVE; }
    public boolean hasSufficientFunds(double amount) { return balance >= amount; }

    /**
     * Credits the account — called only by TransactionService after a
     * Transaction record has been created and validated.
     */
    public void credit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Credit amount must be positive: " + amount);
        this.balance           += amount;
        this.lastTransactionAt  = LocalDateTime.now();
    }

    /**
     * Debits the account — called only by TransactionService after a
     * Transaction record has been created and validated.
     */
    public void debit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Debit amount must be positive: " + amount);
        if (!hasSufficientFunds(amount)) {
            throw new IllegalStateException("Insufficient funds: balance=" + balance + ", requested=" + amount);
        }
        this.balance           -= amount;
        this.lastTransactionAt  = LocalDateTime.now();
    }

    /** Formats balance for display: "47,850.00 ETB" */
    public String formattedBalance() {
        return String.format("%,.2f %s", balance, currency);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public long          getId()                 { return id; }
    public String        getAccountNumber()      { return accountNumber; }
    public long          getUserId()             { return userId; }
    public AccountType   getAccountType()        { return accountType; }
    public AccountStatus getStatus()             { return status; }
    public double        getBalance()            { return balance; }
    public String        getCurrency()           { return currency; }
    public LocalDateTime getOpenedAt()           { return openedAt; }
    public LocalDateTime getLastTransactionAt()  { return lastTransactionAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(long id)                             { this.id = id; }
    public void setAccountNumber(String accountNumber)     { this.accountNumber = accountNumber; }
    public void setUserId(long userId)                     { this.userId = userId; }
    public void setAccountType(AccountType accountType)    { this.accountType = accountType; }
    public void setStatus(AccountStatus status)            { this.status = status; }
    public void setBalance(double balance)                 { this.balance = balance; }
    public void setCurrency(String currency)               { this.currency = currency; }
    public void setOpenedAt(LocalDateTime openedAt)        { this.openedAt = openedAt; }

    @Override
    public String toString() {
        return "Account{number='" + accountNumber + "', type=" + accountType
                + ", balance=" + balance + " " + currency + ", status=" + status + "}";
    }
}