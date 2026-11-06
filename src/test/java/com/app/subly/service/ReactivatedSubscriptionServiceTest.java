package com.app.subly.service;

import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReactivateSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void reactivatesCancelledSubscriptionAndUpdatesPaymentDate() {
        Subscription subscription = givenOwnedCatalogSubscription();

        subscription.reschedule(LocalDate.of(2026, 9, 10));
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.of(2026, 9, 5, 12, 0));

        givenOwnerExists();

        subscriptionService.reactivate(OWNER, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus())
                .isEqualTo(SubscriptionStatus.ACTIVE);

        assertThat(subscription.getCancelledAt())
                .isNull();

        assertThat(subscription.getNextPaymentDate())
                .isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void reactivatingActiveSubscriptionDoesNotChangeIt() {
        Subscription subscription = givenOwnedCatalogSubscription();

        LocalDate originalDate = subscription.getNextPaymentDate();

        subscriptionService.reactivate(OWNER, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus())
                .isEqualTo(SubscriptionStatus.ACTIVE);

        assertThat(subscription.getNextPaymentDate())
                .isEqualTo(originalDate);

        assertThat(subscription.getCancelledAt())
                .isNull();
    }

    @Test
    void memberCannotReactivateSubscription() {
        Subscription subscription = givenSubscriptionOwnedBySomeoneElse();

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.of(2026, 10, 1, 12, 0));

        assertThatThrownBy(() ->
                subscriptionService.reactivate(STRANGER, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionAccessDeniedException.class);

        assertThat(subscription.getStatus())
                .isEqualTo(SubscriptionStatus.CANCELLED);
    }
}
