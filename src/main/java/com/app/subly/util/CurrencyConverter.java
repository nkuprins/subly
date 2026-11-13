package com.app.subly.util;

import com.app.subly.enums.Currency;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CurrencyConverter {

    private static final BigDecimal USD_TO_EUR = new BigDecimal("0.89");

    private static final BigDecimal GBP_TO_EUR = new BigDecimal("1.20");

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
