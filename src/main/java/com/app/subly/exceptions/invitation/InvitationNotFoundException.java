package com.app.subly.exceptions.invitation;

public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException(long invitationId) {
        super("Invitation " + invitationId + " was not found");
    }
}
