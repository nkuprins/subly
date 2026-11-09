package com.app.subly.exception.invitation;

import com.app.subly.exception.BadRequestException;

public class InvitedUserNotFoundException extends BadRequestException {
    public InvitedUserNotFoundException(String usernameOrEmail) {
        super("No user with username or email " + usernameOrEmail);
    }
}
