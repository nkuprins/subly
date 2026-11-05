package com.app.subly.exceptions.invitation;

public class AlreadyMemberException extends RuntimeException {
    public AlreadyMemberException(String username, long subscriptionId) {
        super("User " + username + " is already a member of subscription " + subscriptionId);
    }
}
