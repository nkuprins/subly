package com.app.subly.model.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SubscriptionNextPaymentDateTest {

    @ParameterizedTest(name = "{0} {1} on {2} -> {3}")
    @CsvSource({
        "MONTHLY, 2026-11-20, 2026-11-04, 2026-11-20",
        "MONTHLY, 2026-11-04, 2026-11-04, 2026-11-04",
        "MONTHLY, 2026-11-03, 2026-11-04, 2026-12-03",
        "MONTHLY, 2026-10-04, 2026-11-04, 2026-11-04",
        "MONTHLY, 2024-03-15, 2026-11-04, 2026-11-15",
        "YEARLY,  2026-12-01, 2026-11-04, 2026-12-01",
        "YEARLY,  2026-11-03, 2026-11-04, 2027-11-03",
        "YEARLY,  2025-11-04, 2026-11-04, 2026-11-04",
        "YEARLY,  2020-06-10, 2026-11-04, 2027-06-10",
    })
    void movesForwardByWholeCyclesToTodayOrLater(
            BillingCycle cycle, LocalDate firstPayment, LocalDate today, LocalDate expected) {
        Subscription subscription = subscription(cycle, firstPayment);

        subscription.advancePaymentDate(today);

        assertThat(subscription.getNextPaymentDate()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} {1} on {2} -> {3}")
    @CsvSource({
        "MONTHLY, 2027-01-31, 2027-02-01, 2027-02-28",
        "MONTHLY, 2028-01-31, 2028-02-01, 2028-02-29",
        "MONTHLY, 2027-01-31, 2027-03-01, 2027-03-31",
        "MONTHLY, 2027-01-31, 2027-04-01, 2027-04-30",
        "MONTHLY, 2027-01-31, 2027-04-30, 2027-04-30",
        "YEARLY,  2028-02-29, 2028-03-01, 2029-02-28",
        "YEARLY,  2028-02-29, 2031-03-01, 2032-02-29",
    })
    void keepsOriginalDayOfMonth(BillingCycle cycle, LocalDate firstPayment, LocalDate today, LocalDate expected) {
        Subscription subscription = subscription(cycle, firstPayment);

        subscription.advancePaymentDate(today);

        assertThat(subscription.getNextPaymentDate()).isEqualTo(expected);
    }

    @Test
    void keepsOriginalDayAcrossDailyRuns() {
        Subscription subscription = subscription(BillingCycle.MONTHLY, LocalDate.of(2027, 1, 31));

        subscription.advancePaymentDate(LocalDate.of(2027, 2, 1));
        assertThat(subscription.getNextPaymentDate()).isEqualTo(LocalDate.of(2027, 2, 28));

        subscription.advancePaymentDate(LocalDate.of(2027, 3, 1));
        assertThat(subscription.getNextPaymentDate()).isEqualTo(LocalDate.of(2027, 3, 31));

        subscription.advancePaymentDate(LocalDate.of(2027, 4, 1));
        assertThat(subscription.getNextPaymentDate()).isEqualTo(LocalDate.of(2027, 4, 30));
    }

    @Test
    void rescheduleMovesTheAnchor() {
        Subscription subscription = subscription(BillingCycle.MONTHLY, LocalDate.of(2027, 1, 31));

        subscription.reschedule(LocalDate.of(2027, 3, 15));
        subscription.advancePaymentDate(LocalDate.of(2027, 3, 16));

        assertThat(subscription.getBillingAnchorDate()).isEqualTo(LocalDate.of(2027, 3, 15));
        assertThat(subscription.getNextPaymentDate()).isEqualTo(LocalDate.of(2027, 4, 15));
    }

    private static Subscription subscription(BillingCycle cycle, LocalDate firstPayment) {
        return new Subscription(
                SubscriptionProvider.catalog("Netflix", new Category("Streaming")),
                new BigDecimal("9.99"),
                cycle,
                firstPayment,
                false);
    }
}
