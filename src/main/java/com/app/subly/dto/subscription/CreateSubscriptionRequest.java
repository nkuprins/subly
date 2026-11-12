package com.app.subly.dto.subscription;

import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import com.app.subly.util.MoneyMath;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSubscriptionRequest(
        @NotNull Long providerId,

        @NotNull @Positive @Digits(integer = MoneyMath.INTEGER_DIGITS, fraction = MoneyMath.FRACTION_DIGITS)
        BigDecimal price,

        @NotNull Currency currency,
        @NotNull BillingCycle billingCycle,
        @NotNull LocalDate nextPaymentDate,
        boolean freeTrial,
        @Min(1) int maxParticipants) {}
