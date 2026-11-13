package com.app.subly.exception.subscription;

import com.app.subly.exception.NotFoundException;

public class SubscriptionMemberNotFoundException extends NotFoundException {

    public SubscriptionMemberNotFoundException(long membershipId) {
        super("Subscription membership " + membershipId + " does not exist");
    }
}
