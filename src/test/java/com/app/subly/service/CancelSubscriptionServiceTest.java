package com.app.subly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CancelSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void cancelsActiveSubscription() {
        Subscription subscription = givenOwnedCatalogSubscription();

        LocalDate originalDate = subscription.getNextPaymentDate();

        subscriptionService.cancel(OWNER, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);

        assertThat(subscription.getCancelledAt()).isNotNull();

        assertThat(subscription.getNextPaymentDate()).isEqualTo(originalDate);
    }

    @Test
    void cancellingAlreadyCancelledSubscriptionDoesNotChangeIt() {
        Subscription subscription = givenOwnedCatalogSubscription();

        LocalDateTime originalCancelledAt = LocalDateTime.of(2026, 10, 1, 12, 0);

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(originalCancelledAt);

        subscriptionService.cancel(OWNER, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);

        assertThat(subscription.getCancelledAt()).isEqualTo(originalCancelledAt);
    }

    @Test
    void memberCannotCancelSubscription() {
        Subscription subscription = givenSubscriptionOwnedBySomeoneElse();

        assertThatThrownBy(() -> subscriptionService.cancel(STRANGER, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionAccessDeniedException.class);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        assertThat(subscription.getCancelledAt()).isNull();
    }
}
