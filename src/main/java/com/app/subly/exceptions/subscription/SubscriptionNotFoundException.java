package com.app.subly.exceptions.subscription;

public class SubscriptionNotFoundException extends RuntimeException {
    public SubscriptionNotFoundException(long subscriptionId) {
        super("Subscription " + subscriptionId + " does not exist");
    }
}
