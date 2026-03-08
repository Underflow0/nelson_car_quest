package org.nelson.kidbank.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() {
        super("Insufficient funds — withdrawal would result in a negative balance.");
    }
}
