package com.app.subly.exception.subscription;

import com.app.subly.exception.ConflictException;

public class SubscriptionNotJoinableException extends ConflictException {
    public SubscriptionNotJoinableException(String reason) {
        super(reason);
    }
}
