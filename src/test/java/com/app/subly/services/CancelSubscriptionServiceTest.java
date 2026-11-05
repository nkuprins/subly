package com.app.subly.services;

import com.app.subly.exceptions.subscription.SubscriptionAccessDeniedException;
import com.app.subly.models.subscription.Subscription;
import com.app.subly.models.subscription.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancelSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void cancelsActiveSubscription() {
        Subscription subscription = givenOwnedCatalogSubscription();

        LocalDate originalDate = subscription.getNextPaymentDate();

        subscriptionService.cancel(OWNER, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus())
                .isEqualTo(SubscriptionStatus.CANCELLED);

        assertThat(subscription.getCancelledAt())
                .isNotNull();

        assertThat(subscription.getNextPaymentDate())
                .isEqualTo(originalDate);
    }

    @Test
    void cancellingAlreadyCancelledSubscriptionDoesNotChangeIt() {
        Subscription subscription = givenOwnedCatalogSubscription();

        LocalDateTime originalCancelledAt =
                LocalDateTime.of(2026, 10, 1, 12, 0);

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(originalCancelledAt);

        subscriptionService.cancel(OWNER, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus())
                .isEqualTo(SubscriptionStatus.CANCELLED);

        assertThat(subscription.getCancelledAt())
                .isEqualTo(originalCancelledAt);
    }

    @Test
    void memberCannotCancelSubscription() {
        Subscription subscription = givenSubscriptionOwnedBySomeoneElse();

        assertThatThrownBy(() ->
                subscriptionService.cancel(STRANGER, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionAccessDeniedException.class);

        assertThat(subscription.getStatus())
                .isEqualTo(SubscriptionStatus.ACTIVE);

        assertThat(subscription.getCancelledAt())
                .isNull();
    }
}
