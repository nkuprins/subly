package com.app.subly.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final int INTEGER_DIGITS = 8;

    public static final int FRACTION_DIGITS = 2;

    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);

    private Money() {}

    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(FRACTION_DIGITS, RoundingMode.HALF_UP);
    }

    public static BigDecimal monthlyFromYearly(BigDecimal yearly) {
        return yearly.divide(MONTHS_IN_YEAR, FRACTION_DIGITS, RoundingMode.HALF_UP);
    }
}
