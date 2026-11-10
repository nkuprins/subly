package com.app.subly.model.subscription;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SubscriptionPaymentDatesTest {

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

    private static Subscription monthly(LocalDate nextPaymentDate) {
        return aSubscription().withNextPaymentDate(nextPaymentDate).build();
    }
}
