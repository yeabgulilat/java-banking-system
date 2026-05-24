package com.habeshabank.exception;

/** Thrown when a referenced account does not exist or cannot be loaded. */
public class AccountNotFoundException extends BankingException {

    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}