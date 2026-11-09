package com.app.subly.exception.invitation;

import com.app.subly.exception.ConflictException;

public class InvitationCannotBeAcceptedException extends ConflictException {

    public InvitationCannotBeAcceptedException(String message) {
        super(message);
    }
}
