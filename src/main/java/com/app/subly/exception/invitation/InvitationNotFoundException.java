package com.app.subly.exception.invitation;

public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException(long invitationId) {
        super("Invitation " + invitationId + " was not found");
    }
}
