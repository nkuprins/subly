package com.app.subly.service;

import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReactivateSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void reactivate_shouldActivateCancelledSubscriptionAndMovePaymentDate() {
        Subscription subscription = stubOwnedSubscription(ProviderType.CATALOG);
        subscription.reschedule(LocalDate.of(2026, 9, 10));
        subscription.cancel(Instant.parse("2026-09-05T12:00:00Z"));

        subscriptionService.reactivate(ALICE, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getCancelledAt()).isNull();
        assertThat(subscription.getNextPaymentDate()).isEqualTo(LocalDate.of(2026, 11, 10));
        verifyNoInteractions(userRepository);
    }

    @Test
    void reactivate_shouldKeepActiveSubscriptionUnchanged() {
        Subscription subscription = stubOwnedSubscription(ProviderType.CATALOG);
        LocalDate originalDate = subscription.getNextPaymentDate();

        subscriptionService.reactivate(ALICE, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getNextPaymentDate()).isEqualTo(originalDate);
        assertThat(subscription.getCancelledAt()).isNull();
    }

    @Test
    void reactivate_shouldRejectNonOwner() {
        Subscription subscription = stubSubscriptionOwnedBySomeoneElse();
        subscription.cancel(Instant.parse("2026-10-01T12:00:00Z"));

        assertThatThrownBy(() -> subscriptionService.reactivate(BOB, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionAccessDeniedException.class);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }
}
