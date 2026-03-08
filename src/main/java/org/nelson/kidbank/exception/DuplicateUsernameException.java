package org.nelson.kidbank.exception;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException() {
        super("Username already exists.");
    }
}
