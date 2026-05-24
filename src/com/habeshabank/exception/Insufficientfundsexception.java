package com.habeshabank.exception;

/** Thrown when account balance is too low to complete the operation. */
public class InsufficientFundsException extends BankingException {

    private final double availableBalance;
    private final double requestedAmount;

    public InsufficientFundsException(double availableBalance, double requestedAmount) {
        super(String.format(
                "Insufficient funds: available %.2f ETB, requested %.2f ETB.",
                availableBalance, requestedAmount));
        this.availableBalance = availableBalance;
        this.requestedAmount  = requestedAmount;
    }

    public double getAvailableBalance() { return availableBalance; }
    public double getRequestedAmount()  { return requestedAmount; }
}