package com.app.subly.entity.subscription;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static com.app.subly.testdata.TestUsers.CAROL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.app.subly.testdata.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SubscriptionPaymentDatesTest {

    static final LocalDate PAYMENT_DATE = LocalDate.of(2027, 2, 5);

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
    void paymentsDue_shouldSplitThePriceOnTheNextPaymentDate() {
        Subscription subscription = aSubscription()
                .withPrice("10.00")
                .withNextPaymentDate(PAYMENT_DATE)
                .build();
        SubscriptionMembership owner = SubscriptionMembership.owner(TestUsers.user(ALICE), subscription);
        SubscriptionMembership bob = SubscriptionMembership.member(TestUsers.user(BOB), subscription);
        SubscriptionMembership carol = SubscriptionMembership.member(TestUsers.user(CAROL), subscription);

        assertThat(subscription.paymentsDue(List.of(owner, bob, carol)))
                .extracting(
                        payment -> payment.getUser().getUsername(),
                        SubscriptionPayment::getPaidOn,
                        SubscriptionPayment::getAmount)
                .containsExactlyInAnyOrder(
                        tuple(ALICE, PAYMENT_DATE, new BigDecimal("3.34")),
                        tuple(BOB, PAYMENT_DATE, new BigDecimal("3.33")),
                        tuple(CAROL, PAYMENT_DATE, new BigDecimal("3.33")));
    }

    private static Subscription monthly(LocalDate nextPaymentDate) {
        return aSubscription().withNextPaymentDate(nextPaymentDate).build();
    }
}
