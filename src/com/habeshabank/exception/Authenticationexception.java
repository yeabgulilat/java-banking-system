package com.habeshabank.exception;

/** Thrown when credentials are invalid or the account is locked. */
public class AuthenticationException extends BankingException {

    public AuthenticationException(String message) {
        super(message);
    }
}