package com.app.subly.exception.client;

// is being thrown in a case of 429 Too many requests.
// It can be raised when rate limit or API KEY limit is exceeded
public class RateLimitExceededException extends RuntimeException{
    public RateLimitExceededException(String message, Throwable throwable){
        super(message, throwable);
    }
}
