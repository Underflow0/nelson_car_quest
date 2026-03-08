package org.nelson.kidbank.exception;

public class AccountClosedException extends RuntimeException {
    public AccountClosedException() {
        super("This account is closed and cannot accept transactions.");
    }
}
