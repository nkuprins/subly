package com.app.subly.dto.subscription;

import com.app.subly.enums.BillingCycle;
import com.app.subly.util.Money;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSubscriptionRequest(
        @NotNull Long providerId,

        @NotNull @Positive @Digits(integer = Money.INTEGER_DIGITS, fraction = Money.FRACTION_DIGITS)
        BigDecimal price,

        @NotNull BillingCycle billingCycle,
        @NotNull LocalDate nextPaymentDate,
        boolean freeTrial,
        @Min(1) int maxParticipants) {}
