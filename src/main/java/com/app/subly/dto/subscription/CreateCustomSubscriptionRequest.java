package com.app.subly.dto.subscription;

import com.app.subly.dto.PaymentDetails;
import com.app.subly.model.subscription.BillingCycle;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCustomSubscriptionRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull Long categoryId,

        @NotNull @Positive @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @NotNull BillingCycle billingCycle,
        @NotNull LocalDate nextPaymentDate,
        Boolean freeTrial)
        implements PaymentDetails {

    public CreateCustomSubscriptionRequest {
        freeTrial = Boolean.TRUE.equals(freeTrial);
    }
}
