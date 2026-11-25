package com.app.subly.exception.client;

// represents an error when API key is not recognized by API provider.
// Basically, it is being thrown when 401 Unauthorized occurs
public class InvalidApiKeyException extends RuntimeException {
    public InvalidApiKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
