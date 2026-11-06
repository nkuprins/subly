package com.app.subly.exception.invitation;

public class SelfInvitationException extends RuntimeException {
    public SelfInvitationException() {
        super("You cannot invite yourself");
    }
}
