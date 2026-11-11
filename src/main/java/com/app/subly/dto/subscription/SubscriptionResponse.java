package com.app.subly.dto.subscription;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(
        Long id,
        String providerName,
        BigDecimal price,
        Currency currency,
        BillingCycle billingCycle,
        LocalDate nextPaymentDate,
        boolean freeTrial,
        int maxParticipants) {

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getProvider().getName(),
                subscription.getPrice(),
                subscription.getCurrency(),
                subscription.getBillingCycle(),
                subscription.getNextPaymentDate(),
                subscription.isFreeTrial(),
                subscription.getMaxParticipants());
    }
}
