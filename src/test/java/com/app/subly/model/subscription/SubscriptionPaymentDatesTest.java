package com.app.subly.model.subscription;

import static com.app.subly.testdata.TestClock.NOW;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SubscriptionPaymentDatesTest {

    static final LocalDate FEB_5 = LocalDate.of(2027, 2, 5);

    @ParameterizedTest(name = "ended {0}, today {1} -> {2} skipped")
    @CsvSource({
        "2027-02-05, 2027-02-04, 0",
        "2027-02-05, 2027-02-05, 1",
        "2027-02-05, 2027-03-04, 1",
        "2027-02-05, 2027-03-05, 2",
        "2027-02-05, 2027-03-06, 2",
    })
    void skippedPaymentsUntil_shouldCountDatesFromTheEndDateUpToToday(
            LocalDate endDate, LocalDate today, long expected) {
        assertThat(monthly(endDate).skippedPaymentsUntil(today)).isEqualTo(expected);
    }

    @Test
    void hasEndedBy_shouldBeTrueOnlyForCancelledAfterTheEndDate() {
        Subscription cancelled = monthly(FEB_5);
        cancelled.cancel(NOW);

        assertThat(cancelled.hasEndedBy(FEB_5)).isFalse();
        assertThat(cancelled.hasEndedBy(FEB_5.plusDays(1))).isTrue();
        assertThat(monthly(FEB_5).hasEndedBy(FEB_5.plusDays(1))).isFalse();
    }

    private static Subscription monthly(LocalDate nextPaymentDate) {
        return aSubscription().withNextPaymentDate(nextPaymentDate).build();
    }
}
