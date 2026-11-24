package com.app.subly.enums;

// Defines the priority of putting subscriptions in the email body
// Free trial priorities distinguish subscriptions ending tomorrow from those ending in three days.
// ONE_DAY - 24 hours left till subscription ends
// THREE_DAYS - less than three days left till subscription ends, put this after ONE_DAY
// NONE - this email must not be placed in the email body
public enum EmailSendingPriority {
    FREE_TRIAL_ONE_DAY,
    FREE_TRIAL_THREE_DAYS,
    ONE_DAY,
    THREE_DAYS,
    NONE
}
