package com.app.subly.exceptions;

public class UserIsConnectedError extends RuntimeException {
    public UserIsConnectedError(String message) {
        super(message);
    }
}
