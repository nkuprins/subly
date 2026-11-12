package com.app.subly.dto.payment;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionPayment;
import com.app.subly.enums.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentResponse(String providerName, LocalDate paidOn, BigDecimal amount, Currency currency) {

    public static PaymentResponse from(SubscriptionPayment payment) {
        Subscription subscription = payment.getSubscription();
        return new PaymentResponse(
                subscription.getProvider().getName(),
                payment.getPaidOn(),
                payment.getAmount(),
                subscription.getCurrency());
    }
}
