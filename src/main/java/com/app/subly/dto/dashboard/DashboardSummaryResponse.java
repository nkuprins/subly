package com.app.subly.dto.dashboard;

import com.app.subly.enums.Currency;
import com.app.subly.util.MoneyMath;
import java.math.BigDecimal;

public record DashboardSummaryResponse(
        BigDecimal monthlyTotal, BigDecimal yearlyTotal, Currency currency, int activeCount) {

    public DashboardSummaryResponse {
        monthlyTotal = MoneyMath.round(monthlyTotal);
        yearlyTotal = MoneyMath.round(yearlyTotal);
    }
}
