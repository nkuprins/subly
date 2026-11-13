package com.app.subly.exception.subscription;

import com.app.subly.exception.ConflictException;

public class SubscriptionNotJoinableException extends ConflictException {
    public SubscriptionNotJoinableException(long subscriptionId) {
        super("Subscription " + subscriptionId + " is not open for new members");
    }
}
