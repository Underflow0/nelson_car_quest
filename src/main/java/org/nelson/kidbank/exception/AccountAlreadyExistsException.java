package org.nelson.kidbank.exception;

public class AccountAlreadyExistsException extends RuntimeException {
    public AccountAlreadyExistsException(Long childUserId) {
        super("A savings account already exists for child user id " + childUserId + ".");
    }
}
