package com.app.subly.exception.client;

// represents an error when api key is valid and recognized by API provider,
// but it does not have access for the operation it was provided for.
// Basically, it is being thrown in a case of 403 Forbidden.
public class ApiKeyAccessException extends RuntimeException {
    public ApiKeyAccessException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
