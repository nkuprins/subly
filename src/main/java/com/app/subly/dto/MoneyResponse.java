package com.app.subly.dto;

import com.app.subly.model.subscription.Currency;
import com.app.subly.util.Money;
import java.math.BigDecimal;

public record MoneyResponse(BigDecimal amount, Currency currency) {

    public MoneyResponse {
        amount = Money.round(amount);
    }
}
