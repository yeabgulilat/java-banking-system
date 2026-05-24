package com.habeshabank.model;

/**
 * Holds the current authenticated user session.
 *
 * Phase 1 fields and methods are preserved exactly.
 * Phase 2 adds live object references (User, Account) so services can
 * operate on real domain objects without a second database lookup.
 */
public class UserSession {

    private static UserSession instance;

    // ── Phase 1 flat fields (UI reads these directly — unchanged) ─────────────
    private String accountNumber;
    private String fullName;
    private String email;
    private double balance;
    private String accountType;

    // ── Phase 2 live domain references (used by service layer) ────────────────
    private User liveUser;
    private Account liveAccount;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public static void clearSession() {
        instance = new UserSession();
    }

    // ── Phase 1 Getters ────────────────────────────────────────────────────────

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public double getBalance() {
        return balance;
    }

    public String getAccountType() {
        return accountType;
    }

    // ── Phase 1 Setters ────────────────────────────────────────────────────────

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    /**
     * Updates both the flat balance field (read by UI)
     * AND the live Account object (read by services).
     */
    public void setBalance(double balance) {
        this.balance = balance;

        if (liveAccount != null) {
            liveAccount.setBalance(balance);
        }
    }

    // ── Phase 2 Getters / Setters ──────────────────────────────────────────────

    public User getLiveUser() {
        return liveUser;
    }

    public void setLiveUser(User user) {
        this.liveUser = user;
    }

    public Account getLiveAccount() {
        return liveAccount;
    }

    public void setLiveAccount(Account account) {
        this.liveAccount = account;
    }

    // ── Demo helper ────────────────────────────────────────────────────────────

    /**
     * Populates session with static demo data.
     * Used when bypassing AuthService.
     */
    public void loadDemoUser() {
        this.accountNumber = "ETH-2024-00142";
        this.fullName = "Tigist Alemu";
        this.email = "tigist.alemu@habeshabank.et";
        this.balance = 47850.00;
        this.accountType = "Premium Savings";

        // liveUser / liveAccount remain null
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "account='" + accountNumber + '\'' +
                ", name='" + fullName + '\'' +
                ", balance=" + balance +
                '}';
    }
}