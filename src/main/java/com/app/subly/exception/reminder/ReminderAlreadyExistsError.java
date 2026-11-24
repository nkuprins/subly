package com.app.subly.exception.reminder;

import com.app.subly.exception.ConflictException;

public class ReminderAlreadyExistsError extends ConflictException {
    public ReminderAlreadyExistsError(String message) {
        super(message);
    }
}
