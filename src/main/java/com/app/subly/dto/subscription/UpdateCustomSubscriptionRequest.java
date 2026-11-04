package com.app.subly.dto.subscription;

import com.app.subly.dto.PaymentDetails;
import com.app.subly.models.subscription.BillingCycle;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateCustomSubscriptionRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull Long categoryId,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull BillingCycle billingCycle,
        @NotNull @FutureOrPresent LocalDate nextPaymentDate,
        Boolean freeTrial) implements PaymentDetails {

    public UpdateCustomSubscriptionRequest {
        freeTrial = Boolean.TRUE.equals(freeTrial);
    }
}
