package com.expense_tracker.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistException extends ApiException {
    public UserAlreadyExistException(String message) {

        super(message, HttpStatus.CONFLICT);
    }
}