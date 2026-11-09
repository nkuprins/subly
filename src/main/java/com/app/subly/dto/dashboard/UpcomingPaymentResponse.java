package com.app.subly.dto.dashboard;

import com.app.subly.model.subscription.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpcomingPaymentResponse(
        Long subscriptionId,
        String providerName,
        BigDecimal amount,
        Currency currency,
        LocalDate nextPaymentDate,
        long daysLeft) {}
