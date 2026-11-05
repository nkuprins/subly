package com.app.subly.dto.dashboard;

import com.app.subly.models.subscription.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpcomingPaymentDto(
        Long subscriptionId,
        String serviceName,
        BigDecimal amount,
        Currency currency,
        LocalDate nextPaymentDate,
        long daysLeft) {}
