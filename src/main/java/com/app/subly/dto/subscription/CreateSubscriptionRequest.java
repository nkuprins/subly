package com.app.subly.dto.subscription;

import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Subscription;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSubscriptionRequest(
        @NotNull Long providerId,

        @NotNull
        @Positive
        @Digits(integer = Subscription.PRICE_INTEGER_DIGITS, fraction = Subscription.PRICE_FRACTION_DIGITS)
        BigDecimal price,

        @NotNull BillingCycle billingCycle,
        @NotNull LocalDate nextPaymentDate,
        Boolean freeTrial,
        @Min(1) Integer maxParticipants)
        implements PaymentDetails {

    public CreateSubscriptionRequest {
        freeTrial = Boolean.TRUE.equals(freeTrial);
        maxParticipants = maxParticipants == null ? 1 : maxParticipants;
    }

    public CreateSubscriptionRequest(
            Long providerId,
            BigDecimal price,
            BillingCycle billingCycle,
            LocalDate nextPaymentDate,
            Boolean freeTrial) {
        this(providerId, price, billingCycle, nextPaymentDate, freeTrial, 1);
    }
}
