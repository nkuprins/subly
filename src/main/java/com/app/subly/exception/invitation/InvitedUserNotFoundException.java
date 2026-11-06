package com.app.subly.exception.invitation;

public class InvitedUserNotFoundException extends RuntimeException {
    public InvitedUserNotFoundException(String usernameOrEmail) {
        super("No user with username or email " + usernameOrEmail);
    }
}
