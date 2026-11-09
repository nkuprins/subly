package com.app.subly.exception.invitation;

import com.app.subly.exception.ConflictException;

public class AlreadyMemberException extends ConflictException {
    public AlreadyMemberException(String username, long subscriptionId) {
        super("User " + username + " is already a member of subscription " + subscriptionId);
    }
}
