package com.app.subly.exception.subscription;

import com.app.subly.exception.ForbiddenException;

public class SubscriptionAccessDeniedException extends ForbiddenException {
    public SubscriptionAccessDeniedException(long subscriptionId) {
        super("Access denied: only the owner can change subscription " + subscriptionId);
    }
}
