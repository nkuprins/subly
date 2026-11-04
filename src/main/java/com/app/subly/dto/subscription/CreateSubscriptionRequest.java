package com.app.subly.dto.subscription;

import com.app.subly.dto.PaymentDetails;
import com.app.subly.models.subscription.BillingCycle;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSubscriptionRequest(
        @NotNull Long providerId,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull BillingCycle billingCycle,
        @NotNull @FutureOrPresent LocalDate nextPaymentDate,
        Boolean freeTrial) implements PaymentDetails {

    public CreateSubscriptionRequest {
        freeTrial = Boolean.TRUE.equals(freeTrial);
    }
}
