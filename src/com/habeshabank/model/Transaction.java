package com.habeshabank.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single banking transaction record.
 */
public class Transaction {

    public enum Type {
        DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, EQUB, IDDIR
    }

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    private long            id;
    private String          referenceNumber;
    private Type            type;
    private double          amount;
    private double          balanceAfter;
    private String          description;
    private String          counterpartyAccount;
    private LocalDateTime   timestamp;

    public Transaction() {}

    public Transaction(Type type, double amount, double balanceAfter,
                       String description, String counterpartyAccount) {
        this.type               = type;
        this.amount             = amount;
        this.balanceAfter       = balanceAfter;
        this.description        = description;
        this.counterpartyAccount = counterpartyAccount;
        this.timestamp          = LocalDateTime.now();
        this.referenceNumber    = generateRef();
    }

    private String generateRef() {
        return "HB" + System.currentTimeMillis() % 1_000_000_000L;
    }

    public String formattedTimestamp() {
        return timestamp != null ? timestamp.format(DISPLAY_FORMAT) : "";
    }

    public String signedAmount() {
        return switch (type) {
            case DEPOSIT, TRANSFER_IN, EQUB, IDDIR -> String.format("+%.2f ETB", amount);
            case WITHDRAWAL, TRANSFER_OUT           -> String.format("-%.2f ETB", amount);
        };
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public long            getId()                   { return id; }
    public void            setId(long id)            { this.id = id; }

    public String          getReferenceNumber()      { return referenceNumber; }
    public void            setReferenceNumber(String r){ this.referenceNumber = r; }

    public Type            getType()                 { return type; }
    public void            setType(Type type)        { this.type = type; }

    public double          getAmount()               { return amount; }
    public void            setAmount(double amount)  { this.amount = amount; }

    public double          getBalanceAfter()         { return balanceAfter; }
    public void            setBalanceAfter(double b) { this.balanceAfter = b; }

    public String          getDescription()          { return description; }
    public void            setDescription(String d)  { this.description = d; }

    public String          getCounterpartyAccount()  { return counterpartyAccount; }
    public void            setCounterpartyAccount(String a) { this.counterpartyAccount = a; }

    public LocalDateTime   getTimestamp()            { return timestamp; }
    public void            setTimestamp(LocalDateTime t){ this.timestamp = t; }
}
