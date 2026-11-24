package com.app.subly.exception.client;

// General exception for representing occurred errors on the API provider server side
// Usually 5xx codes.
public class APIServerException extends RuntimeException {
    public APIServerException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
