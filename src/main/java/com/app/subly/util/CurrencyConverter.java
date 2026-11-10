package com.app.subly.util;

import com.app.subly.model.subscription.Currency;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CurrencyConverter {

    private static final BigDecimal USD_TO_EUR = new BigDecimal("0.89");

    private static final BigDecimal GBP_TO_EUR = new BigDecimal("1.20");

    private CurrencyConverter() {}

    public static BigDecimal toEur(BigDecimal amount, Currency currency) {
        return switch (currency) {
            case EUR -> amount;
            case USD -> amount.multiply(USD_TO_EUR);
            case GBP -> amount.multiply(GBP_TO_EUR);
        };
    }

    public static BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        if (from == to) {
            return amount;
        }

        BigDecimal amountEur = toEur(amount, from);
        return switch (to) {
            case EUR -> amountEur;
            case USD -> amountEur.divide(USD_TO_EUR, 12, RoundingMode.HALF_UP);
            case GBP -> amountEur.divide(GBP_TO_EUR, 12, RoundingMode.HALF_UP);
        };
    }
}
