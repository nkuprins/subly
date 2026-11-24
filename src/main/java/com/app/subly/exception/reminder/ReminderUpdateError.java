package com.app.subly.exception.reminder;

import com.app.subly.exception.ForbiddenException;

public class ReminderUpdateError extends ForbiddenException {
    public ReminderUpdateError(String message){
        super(message);
    }
}
