package com.app.subly.dto.payment;

import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionPayment;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentResponse(
        Long subscriptionId, String providerName, LocalDate paidOn, BigDecimal amount, Currency currency) {

    public static PaymentResponse from(SubscriptionPayment payment) {
        Subscription subscription = payment.getSubscription();
        return new PaymentResponse(
                subscription.getId(),
                subscription.getProvider().getName(),
                payment.getPaidOn(),
                payment.getAmount(),
                subscription.getCurrency());
    }
}
