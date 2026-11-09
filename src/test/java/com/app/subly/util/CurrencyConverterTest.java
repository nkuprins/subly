package com.app.subly.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.model.subscription.Currency;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CurrencyConverterTest {

    @ParameterizedTest(name = "{0} {1} -> {2} EUR")
    @CsvSource({
        "10.00, EUR, 10.00",
        "10.00, USD, 8.90",
        "10.00, GBP, 12.00",
        "1.01,  USD, 0.8989",
    })
    void toEur_shouldConvertAtTheFixedRateWithoutRounding(BigDecimal amount, Currency currency, BigDecimal expected) {
        assertThat(CurrencyConverter.toEur(amount, currency)).isEqualByComparingTo(expected);
    }
}
