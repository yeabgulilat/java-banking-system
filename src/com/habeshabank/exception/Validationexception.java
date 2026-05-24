package com.habeshabank.exception;

/** Thrown when input fails domain validation rules (amounts, limits, fields). */
public class ValidationException extends BankingException {

    private final String field;   // optional — which field failed

    public ValidationException(String message) {
        super(message);
        this.field = null;
    }

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}