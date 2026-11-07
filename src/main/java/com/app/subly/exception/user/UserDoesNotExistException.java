package com.app.subly.exception.user;

import com.app.subly.exception.NotFoundException;

public class UserDoesNotExistException extends NotFoundException {
    public UserDoesNotExistException(String message) {
        super(message);
    }
}
