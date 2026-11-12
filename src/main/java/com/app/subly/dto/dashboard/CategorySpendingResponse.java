package com.app.subly.dto.dashboard;

import com.app.subly.enums.Currency;
import com.app.subly.util.MoneyMath;
import java.math.BigDecimal;

public record CategorySpendingResponse(
        String categoryName, BigDecimal monthlyTotal, BigDecimal yearlyTotal, Currency currency) {

    public CategorySpendingResponse {
        monthlyTotal = MoneyMath.round(monthlyTotal);
        yearlyTotal = MoneyMath.round(yearlyTotal);
    }
}
