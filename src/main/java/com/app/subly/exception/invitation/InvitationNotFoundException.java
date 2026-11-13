package com.app.subly.exception.invitation;

import com.app.subly.exception.NotFoundException;

public class InvitationNotFoundException extends NotFoundException {

    public InvitationNotFoundException(long invitationId) {
        super("Invitation " + invitationId + " was not found");
    }
}
