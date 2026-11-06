package com.app.subly.exception.user;

import com.app.subly.exception.BadRequestException;

public class UserInfoUpdateException extends BadRequestException {
    public UserInfoUpdateException(String message) {
        super(message);
    }
}
