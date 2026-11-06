package com.app.subly.exception.invitation;

import com.app.subly.exception.BadRequestException;

public class SelfInvitationException extends BadRequestException {
    public SelfInvitationException() {
        super("You cannot invite yourself");
    }
}
