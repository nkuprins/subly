package com.app.subly.dto.dashboard;

import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record UpcomingPaymentResponse(
        Long subscriptionId,
        String providerName,
        BigDecimal amount,
        Currency currency,
        LocalDate nextPaymentDate,
        long daysLeft) {

    public UpcomingPaymentResponse {
        amount = Money.round(amount);
    }

    public static UpcomingPaymentResponse from(
            Subscription subscription, BigDecimal amount, Currency currency, LocalDate today) {
        return new UpcomingPaymentResponse(
                subscription.getId(),
                subscription.getProvider().getName(),
                amount,
                currency,
                subscription.getNextPaymentDate(),
                ChronoUnit.DAYS.between(today, subscription.getNextPaymentDate()));
    }
}
