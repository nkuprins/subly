package com.app.subly.util;

import com.app.subly.dto.reminder.ReminderDTO;
import com.app.subly.enums.EmailSendingPriority;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
public class EmailSendingPolicy {
    public static EmailSendingPriority defineReminderSendingPriority(ReminderDTO reminder){
        // This method decides whether the email for this reminder should be sent.
        // It calculates left days the subscription will be expired after, and return priority
        // (since all the expired-soon subscriptions must be grouped in one email by their expiration time)

        if(reminder.getFirstSentAt() != null && reminder.getSecondSentAt() != null){
            return EmailSendingPriority.NONE;
        }

        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(),
                reminder.getSubscriptionNextPaymentDate());
        log.debug("Days left for {} is: {}", reminder.getSubscriptionNextPaymentDate(), daysLeft);
        // subscription is expired
        if(daysLeft < 0){
            return EmailSendingPriority.NONE;
        }

        if(daysLeft <= 1 && reminder.getFirstSentAt() == null){
            return reminder.isFreeTrial()
                    ? EmailSendingPriority.FREE_TRIAL_ONE_DAY
                    : EmailSendingPriority.ONE_DAY;
        }

        if(daysLeft <= 3 && reminder.getSecondSentAt() == null){
            if(reminder.getFirstSentAt() != null){
                // prevents duplicity when a user adds a subscriptions that ends tomorrow
                // in this case, one-day email had been sent before we reached this block.
                return EmailSendingPriority.NONE;
            }
            return reminder.isFreeTrial()
                    ? EmailSendingPriority.FREE_TRIAL_THREE_DAYS
                    : EmailSendingPriority.THREE_DAYS;
        }
        // 2) проверить логику
        return EmailSendingPriority.NONE;
    }
}
