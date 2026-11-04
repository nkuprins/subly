package com.app.subly.dashboard;

import com.app.subly.models.subscription.Currency;
import com.app.subly.services.CurrencyConverter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyConverterTest {

    @Test
    void eurIsNotConverted() {
        BigDecimal result = CurrencyConverter.toEur(new BigDecimal("10.00"), Currency.EUR);
        assertEquals(0, result.compareTo(new BigDecimal("10.00")));
    }

    @Test
    void convertsUsdToEur() {
        BigDecimal result = CurrencyConverter.toEur(new BigDecimal("10.00"), Currency.USD);
        assertEquals(0, result.compareTo(new BigDecimal("8.90")));
    }

    @Test
    void convertsGbpToEur() {
        BigDecimal result = CurrencyConverter.toEur(new BigDecimal("10.00"), Currency.GBP);
        assertEquals(0, result.compareTo(new BigDecimal("12.00")));
    }

    @Test
    void doesNotRoundDuringConversion() {
        BigDecimal result = CurrencyConverter.toEur(new BigDecimal("1.01"), Currency.USD);
        assertEquals(0, result.compareTo(new BigDecimal("0.8989")));
    }
}
