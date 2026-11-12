package com.app.subly.service;

import static com.app.subly.testdata.TestClock.NOW;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_NEXT_PAYMENT_DATE;
import static com.app.subly.testdata.TestSubscriptions.SUBSCRIPTION_ID;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.enums.SubscriptionStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SubscriptionLifecycleServiceTest extends AbstractSubscriptionServiceTest {

    private static final LocalDate PAST_PAYMENT_DATE = DEFAULT_NEXT_PAYMENT_DATE.minusMonths(2);

    @Test
    void cancel_shouldCancelActiveSubscription() {
        Subscription subscription = subscription(catalogProvider());
        stubOwnedSubscription(subscription);

        service.cancel(ALICE, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(subscription.getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void cancel_shouldKeepAlreadyCancelledSubscriptionUnchanged() {
        Subscription subscription = subscription(catalogProvider());
        stubOwnedSubscription(subscription);
        Instant originalCancelledAt = NOW.minus(Duration.ofDays(1));
        subscription.cancel(originalCancelledAt);

        service.cancel(ALICE, SUBSCRIPTION_ID);

        assertThat(subscription.getCancelledAt()).isEqualTo(originalCancelledAt);
    }

    @Test
    void reactivate_shouldActivateCancelledSubscriptionAndMovePaymentDate() {
        Subscription subscription = subscription(catalogProvider());
        stubOwnerMembership(subscription);
        subscription.reschedule(PAST_PAYMENT_DATE);
        subscription.cancel(NOW);

        service.reactivate(ALICE, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getCancelledAt()).isNull();
        assertThat(subscription.getNextPaymentDate()).isEqualTo(PAST_PAYMENT_DATE.plusMonths(2));
    }

    @Test
    void reactivate_shouldKeepActiveSubscriptionUnchanged() {
        Subscription subscription = subscription(catalogProvider());
        stubOwnerMembership(subscription);
        subscription.reschedule(PAST_PAYMENT_DATE);

        service.reactivate(ALICE, SUBSCRIPTION_ID);

        assertThat(subscription.getNextPaymentDate()).isEqualTo(PAST_PAYMENT_DATE);
    }
}
