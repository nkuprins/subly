package com.app.subly.dto;

import com.app.subly.enums.Currency;
import com.app.subly.util.MoneyMath;
import java.math.BigDecimal;

public record MoneyResponse(BigDecimal amount, Currency currency) {

    public MoneyResponse {
        amount = MoneyMath.round(amount);
    }
}
