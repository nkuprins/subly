package com.app.subly.service;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.Currency;
import com.app.subly.util.CurrencyConverter;
import java.math.BigDecimal;
import java.time.LocalDate;

record Share(SubscriptionMembership membership, BigDecimal amount) {

    Subscription subscription() {
        return membership.getSubscription();
    }

    BigDecimal savedUntil(LocalDate today) {
        return amount.multiply(BigDecimal.valueOf(subscription().skippedPaymentsUntil(today)));
    }

    BigDecimal savedUntil(LocalDate today, Currency target) {
        return toCurrency(savedUntil(today), target);
    }

    BigDecimal amountIn(Currency target) {
        return toCurrency(amount, target);
    }

    BigDecimal yearlyIn(Currency target) {
        int paymentsPerYear = subscription().getBillingCycle().getPaymentsPerYear();
        return amountIn(target).multiply(BigDecimal.valueOf(paymentsPerYear));
    }

    private BigDecimal toCurrency(BigDecimal value, Currency target) {
        return CurrencyConverter.convert(value, subscription().getCurrency(), target);
    }
}
