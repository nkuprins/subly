package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.ActiveSubscriptionResponse;
import com.app.subly.dto.subscription.CancelledSubscriptionResponse;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.SubscriptionStatus;
import com.app.subly.exception.subscription.SubscriptionNotActiveException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void findActive_shouldReturnSubscriptionsWithMembershipRolesAndUsersShare() {
        SubscriptionMembership ownerMembership = SubscriptionMembership.owner(alice, subscription(customProvider()));
        SubscriptionMembership memberMembership = SubscriptionMembership.member(alice, subscription(catalogProvider()));
        when(subscriptionShares.forUser(ALICE, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(
                        new Share(ownerMembership, new BigDecimal("6.00")),
                        new Share(memberMembership, new BigDecimal("3.00"))));

        assertThat(service.findActive(ALICE))
                .containsExactly(
                        ActiveSubscriptionResponse.from(ownerMembership, new BigDecimal("6.00")),
                        ActiveSubscriptionResponse.from(memberMembership, new BigDecimal("3.00")));
    }

    @Test
    void getActive_shouldReturnOneActiveSubscriptionOfTheUser() {
        Subscription subscription = aSubscription().withId(SUBSCRIPTION_ID).build();
        stubAlicesMembership(SubscriptionMembership.member(alice, subscription));

        assertThat(service.getActive(ALICE, SUBSCRIPTION_ID)).isEqualTo(SubscriptionResponse.from(subscription));
    }

    @Test
    void getActive_shouldRejectCancelledSubscription() {
        stubAlicesMembership(SubscriptionMembership.owner(alice, cancelled(SUBSCRIPTION_ID, TODAY.plusDays(3))));

        assertThatThrownBy(() -> service.getActive(ALICE, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionNotActiveException.class);
    }

    @Test
    void findCancelled_shouldReturnEveryCancelledSubscriptionWithItsSavingsSoFar() {
        SubscriptionMembership ended = SubscriptionMembership.owner(alice, cancelled(1L, TODAY.minusMonths(1)));
        SubscriptionMembership endsLater = SubscriptionMembership.owner(alice, cancelled(2L, TODAY.plusDays(3)));
        stubAliceExists();
        when(subscriptionShares.forUser(ALICE, SubscriptionStatus.CANCELLED))
                .thenReturn(List.of(
                        new Share(ended, new BigDecimal("5.00")), new Share(endsLater, new BigDecimal("5.00"))));

        assertThat(service.findCancelled(ALICE))
                .extracting(cancelled -> cancelled.subscription().id(), CancelledSubscriptionResponse::saved)
                .containsExactly(tuple(1L, new BigDecimal("10.00")), tuple(2L, new BigDecimal("0.00")));
    }

    private void stubAlicesMembership(SubscriptionMembership membership) {
        when(subscriptionAccess.getMembership(ALICE, SUBSCRIPTION_ID)).thenReturn(membership);
    }

    private static Subscription cancelled(long id, LocalDate endDate) {
        return aSubscription()
                .withId(id)
                .withNextPaymentDate(endDate)
                .cancelled()
                .build();
    }
}
