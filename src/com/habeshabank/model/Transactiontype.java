package com.habeshabank.model;

/**
 * Canonical enum for all transaction categories in the system.
 *
 * Extracted as a top-level type so services and repositories can reference
 * it without importing the full Transaction class.
 *
 * NOTE: Transaction.Type (the inline enum from Phase 1) is preserved for
 * backward compatibility with existing UI code. New service/model code should
 * use this top-level enum exclusively.
 */
public enum TransactionType {

    DEPOSIT       ("Deposit",        true),
    WITHDRAWAL    ("Withdrawal",     false),
    TRANSFER_IN   ("Transfer In",    true),
    TRANSFER_OUT  ("Transfer Out",   false),
    EQUB          ("Equb",           false),   // net outflow; pot win recorded as TRANSFER_IN
    IDDIR         ("Iddir",          false);   // mutual-aid contribution outflow

    private final String displayName;
    private final boolean isCredit;   // true = money coming in, false = money going out

    TransactionType(String displayName, boolean isCredit) {
        this.displayName = displayName;
        this.isCredit    = isCredit;
    }

    public String getDisplayName() { return displayName; }
    public boolean isCredit()      { return isCredit; }
    public boolean isDebit()       { return !isCredit; }

    /** Signed prefix used in UI display ("+" or "−"). */
    public String signPrefix() { return isCredit ? "+" : "-"; }

    /**
     * Converts a legacy Transaction.Type to this enum.
     * Allows existing UI code that uses Transaction.Type to interoperate
     * with service-layer results returned as TransactionType.
     */
    public static TransactionType from(Transaction.Type legacy) {
        return switch (legacy) {
            case DEPOSIT      -> DEPOSIT;
            case WITHDRAWAL   -> WITHDRAWAL;
            case TRANSFER_IN  -> TRANSFER_IN;
            case TRANSFER_OUT -> TRANSFER_OUT;
            case EQUB         -> EQUB;
            case IDDIR        -> IDDIR;
        };
    }
}