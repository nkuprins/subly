package com.app.subly.exception.user;

import com.app.subly.exception.ConflictException;

public class UserIsConnectedException extends ConflictException {
    public UserIsConnectedException(String message) {
        super(message);
    }
}
