package com.app.subly.dto.dashboard;

import com.app.subly.model.subscription.Currency;
import com.app.subly.util.Money;
import java.math.BigDecimal;

public record DashboardSummaryResponse(
        BigDecimal monthlyTotal, BigDecimal yearlyTotal, Currency currency, int activeCount) {

    public DashboardSummaryResponse {
        monthlyTotal = Money.round(monthlyTotal);
        yearlyTotal = Money.round(yearlyTotal);
    }
}
