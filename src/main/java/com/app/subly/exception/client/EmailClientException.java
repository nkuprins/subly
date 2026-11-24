package com.app.subly.exception.client;

// General exception for representing the errors that are not mentioned in the
// default provided exceptions.
// Use it only if no one provided exception in this package fits your error.
public class EmailClientException extends RuntimeException {
    public EmailClientException(String message, Throwable throwable){
        super(message, throwable);
    }
}
