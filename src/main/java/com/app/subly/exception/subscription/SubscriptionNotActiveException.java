package com.app.subly.exception.subscription;

import com.app.subly.exception.ConflictException;

public class SubscriptionNotActiveException extends ConflictException {
    public SubscriptionNotActiveException(long subscriptionId) {
        super("Subscription " + subscriptionId + " is not active");
    }
}
