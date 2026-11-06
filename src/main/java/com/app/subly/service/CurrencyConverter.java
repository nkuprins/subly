package com.app.subly.service;

import com.app.subly.model.subscription.Currency;

import java.math.BigDecimal;

public final class CurrencyConverter {

    private static final BigDecimal USD_TO_EUR =
            new BigDecimal("0.89");

    private static final BigDecimal GBP_TO_EUR =
            new BigDecimal("1.20");

    private CurrencyConverter() {}

    public static BigDecimal toEur(
            BigDecimal amount,
            Currency currency
    ) {
        return switch (currency) {
            case EUR -> amount;
            case USD -> amount.multiply(USD_TO_EUR);
            case GBP -> amount.multiply(GBP_TO_EUR);
        };
    }
}
