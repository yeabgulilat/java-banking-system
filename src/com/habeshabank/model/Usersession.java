package com.habeshabank.model;

/**
 * Holds the current authenticated user session.
 * Will be populated from the database during login.
 */
public class UserSession {

    private static UserSession instance;

    private String accountNumber;
    private String fullName;
    private String email;
    private double balance;
    private String accountType;  // e.g. "Savings", "Current"

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

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getAccountNumber() { return accountNumber; }
    public String getFullName()      { return fullName; }
    public String getEmail()         { return email; }
    public double getBalance()       { return balance; }
    public String getAccountType()   { return accountType; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public void setFullName(String fullName)           { this.fullName = fullName; }
    public void setEmail(String email)                 { this.email = email; }
    public void setBalance(double balance)             { this.balance = balance; }
    public void setAccountType(String accountType)     { this.accountType = accountType; }

    /** Convenience: populate with demo data for UI-only mode */
    public void loadDemoUser() {
        this.accountNumber = "ETH-2024-00142";
        this.fullName      = "Tigist Alemu";
        this.email         = "tigist.alemu@habeshabank.et";
        this.balance       = 47_850.00;
        this.accountType   = "Premium Savings";
    }

    @Override
    public String toString() {
        return "UserSession{account='" + accountNumber + "', name='" + fullName + "', balance=" + balance + "}";
    }
}