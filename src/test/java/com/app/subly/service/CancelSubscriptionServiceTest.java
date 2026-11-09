package com.app.subly.service;

import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CancelSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void cancel_shouldCancelActiveSubscription() {
        Subscription subscription = stubOwnedSubscription(ProviderType.CATALOG);
        LocalDate originalDate = subscription.getNextPaymentDate();

        subscriptionService.cancel(ALICE, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(subscription.getCancelledAt()).isNotNull();
        assertThat(subscription.getNextPaymentDate()).isEqualTo(originalDate);
    }

    @Test
    void cancel_shouldKeepAlreadyCancelledSubscriptionUnchanged() {
        Subscription subscription = stubOwnedSubscription(ProviderType.CATALOG);
        Instant originalCancelledAt = Instant.parse("2026-10-01T12:00:00Z");
        subscription.cancel(originalCancelledAt);

        subscriptionService.cancel(ALICE, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(subscription.getCancelledAt()).isEqualTo(originalCancelledAt);
    }

    @Test
    void cancel_shouldRejectNonOwner() {
        Subscription subscription = stubSubscriptionOwnedBySomeoneElse();

        assertThatThrownBy(() -> subscriptionService.cancel(BOB, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionAccessDeniedException.class);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getCancelledAt()).isNull();
    }
}
