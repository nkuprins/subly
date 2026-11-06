package com.app.subly.exception.subscription;

public class SubscriptionAccessDeniedException extends RuntimeException {
    public SubscriptionAccessDeniedException(long subscriptionId) {
        super("Access denied: only the owner can change subscription " + subscriptionId);
    }
}
