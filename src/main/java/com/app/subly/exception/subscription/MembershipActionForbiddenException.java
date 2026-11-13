package com.app.subly.exception.subscription;

import com.app.subly.exception.ForbiddenException;

public class MembershipActionForbiddenException extends ForbiddenException {

    public MembershipActionForbiddenException(String message) {
        super(message);
    }
}
