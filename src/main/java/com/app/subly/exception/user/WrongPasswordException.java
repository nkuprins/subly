package com.app.subly.exception.user;

import com.app.subly.exception.BadRequestException;

public class WrongPasswordException extends BadRequestException {
    public WrongPasswordException(String message) {
        super(message);
    }
}
