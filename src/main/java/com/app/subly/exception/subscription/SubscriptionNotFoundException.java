package com.app.subly.exception.subscription;

import com.app.subly.exception.NotFoundException;

public class SubscriptionNotFoundException extends NotFoundException {
    public SubscriptionNotFoundException(long subscriptionId) {
        super("Subscription " + subscriptionId + " does not exist");
    }
}
