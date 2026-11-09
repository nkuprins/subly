package com.app.subly.exception;

public abstract class ConflictException extends RuntimeException {
    protected ConflictException(String message) {
        super(message);
    }
}
