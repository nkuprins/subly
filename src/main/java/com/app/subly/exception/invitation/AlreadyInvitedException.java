package com.app.subly.exception.invitation;

import com.app.subly.exception.ConflictException;

public class AlreadyInvitedException extends ConflictException {
    public AlreadyInvitedException(String username, long subscriptionId) {
        super("User " + username + " already has a pending invitation to subscription " + subscriptionId);
    }
}
