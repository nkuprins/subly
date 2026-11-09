package com.app.subly.exception.user;

import com.app.subly.exception.ConflictException;

public class UserAlreadyExistsException extends ConflictException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
