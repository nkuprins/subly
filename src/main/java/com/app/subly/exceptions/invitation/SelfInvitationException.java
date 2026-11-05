package com.app.subly.exceptions.invitation;

public class SelfInvitationException extends RuntimeException {
    public SelfInvitationException() {
        super("You cannot invite yourself");
    }
}
