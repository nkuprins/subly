package com.app.subly.dto.dashboard;

import com.app.subly.model.subscription.Currency;

import java.math.BigDecimal;

public record DashboardSummaryDto(
        BigDecimal monthlyTotal,
        BigDecimal yearlyTotal,
        Currency currency) {}
