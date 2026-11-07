package com.app.subly.dto.subscription;

import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Subscription;
import jakarta.validation.constraints.Digits;
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
        Boolean freeTrial)
        implements PaymentDetails {

    public CreateSubscriptionRequest {
        freeTrial = Boolean.TRUE.equals(freeTrial);
    }
}
