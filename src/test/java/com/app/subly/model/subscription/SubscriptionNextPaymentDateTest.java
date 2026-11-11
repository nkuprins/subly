package com.app.subly.model.subscription;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SubscriptionNextPaymentDateTest {

    @ParameterizedTest(name = "{0} {1} on {2} -> {3}")
    @CsvSource({
        "MONTHLY, 2027-01-20, 2026-11-04, 2027-01-20", // more than a cycle ahead: unchanged
        "MONTHLY, 2026-11-20, 2026-11-04, 2026-11-20", // later this cycle: unchanged
        "MONTHLY, 2026-11-04, 2026-11-04, 2026-11-04", // due today: stays today
        "MONTHLY, 2026-11-03, 2026-11-04, 2026-12-03", // due yesterday: one month later
        "MONTHLY, 2026-10-04, 2026-11-04, 2026-11-04", // exactly one cycle ago: lands on today
        "MONTHLY, 2024-03-15, 2026-11-04, 2026-11-15", // years behind: first 15th from today
        "YEARLY,  2026-11-03, 2026-11-04, 2027-11-03", // yearly: one year later
        "MONTHLY, 2027-01-31, 2027-04-30, 2027-04-30", // 31st in a 30-day month: last day
        "MONTHLY, 2028-01-31, 2028-02-01, 2028-02-29", // 31st in leap February: February 29
        "YEARLY,  2028-02-29, 2028-03-01, 2029-02-28", // February 29 in a common year: February 28
        "YEARLY,  2028-02-29, 2031-03-01, 2032-02-29", // next leap year: back to February 29
    })
    void advancePaymentDate_shouldMoveByWholeCyclesKeepingTheDayOfMonth(
            BillingCycle cycle, LocalDate anchorDate, LocalDate today, LocalDate expected) {
        Subscription subscription = subscription(cycle, anchorDate);

        subscription.advancePaymentDate(today);

        assertThat(subscription.getNextPaymentDate()).isEqualTo(expected);
    }

    @Test
    void advancePaymentDate_shouldKeepOriginalDayAcrossDailyRuns() {
        Subscription subscription = subscription(BillingCycle.MONTHLY, LocalDate.of(2027, 1, 31));

        subscription.advancePaymentDate(LocalDate.of(2027, 2, 1));
        assertThat(subscription.getNextPaymentDate()).isEqualTo(LocalDate.of(2027, 2, 28));

        subscription.advancePaymentDate(LocalDate.of(2027, 3, 1));
        assertThat(subscription.getNextPaymentDate()).isEqualTo(LocalDate.of(2027, 3, 31));

        subscription.advancePaymentDate(LocalDate.of(2027, 4, 1));
        assertThat(subscription.getNextPaymentDate()).isEqualTo(LocalDate.of(2027, 4, 30));
    }

    @Test
    void reschedule_shouldMoveTheAnchor() {
        Subscription subscription = subscription(BillingCycle.MONTHLY, LocalDate.of(2027, 1, 31));
        LocalDate newAnchor = LocalDate.of(2027, 3, 15);

        subscription.reschedule(newAnchor);
        subscription.moveToNextCycle();

        assertThat(subscription.getBillingAnchorDate()).isEqualTo(newAnchor);
        assertThat(subscription.getNextPaymentDate()).isEqualTo(newAnchor.plusMonths(1));
    }

    private static Subscription subscription(BillingCycle cycle, LocalDate anchorDate) {
        return aSubscription()
                .withBillingCycle(cycle)
                .withNextPaymentDate(anchorDate)
                .build();
    }
}
