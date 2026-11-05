package com.app.subly.exceptions.invitation;

public class InvitedUserNotFoundException extends RuntimeException {
    public InvitedUserNotFoundException(String usernameOrEmail) {
        super("No user with username or email " + usernameOrEmail);
    }
}
