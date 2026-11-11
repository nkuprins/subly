package com.app.subly.dto.dashboard;

import com.app.subly.enums.Currency;
import com.app.subly.util.Money;
import java.math.BigDecimal;

public record CategorySpendingResponse(
        Long categoryId, String categoryName, BigDecimal monthlyTotal, BigDecimal yearlyTotal, Currency currency) {

    public CategorySpendingResponse {
        monthlyTotal = Money.round(monthlyTotal);
        yearlyTotal = Money.round(yearlyTotal);
    }
}
