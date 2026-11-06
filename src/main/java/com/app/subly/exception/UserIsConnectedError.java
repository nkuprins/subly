package com.app.subly.exception;

public class UserIsConnectedError extends RuntimeException {
    public UserIsConnectedError(String message) {
        super(message);
    }
}
