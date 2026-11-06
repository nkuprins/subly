package com.app.subly.exception.invitation;

public class AlreadyInvitedException extends RuntimeException {
    public AlreadyInvitedException(String username, long subscriptionId) {
        super("User " + username + " already has a pending invitation to subscription " + subscriptionId);
    }
}
