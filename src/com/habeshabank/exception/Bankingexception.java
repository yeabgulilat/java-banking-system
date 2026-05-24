package com.habeshabank.exception;

/**
 * Root checked exception for all Habesha Bank domain errors.
 * Services throw subclasses; UI catches this base type for generic handling.
 */
public class BankingException extends Exception {

    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}