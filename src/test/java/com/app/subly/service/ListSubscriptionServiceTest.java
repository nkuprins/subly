package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.CancelledSubscriptionResponse;
import com.app.subly.dto.subscription.VisibleSubscriptionResponse;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.service.SubscriptionShares.Share;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void findCurrent_shouldReturnSubscriptionsWithMembershipRoles() {
        Subscription owned = subscription(customProvider());
        Subscription shared = subscription(catalogProvider());
        SubscriptionMembership ownerMembership = SubscriptionMembership.owner(alice, owned);
        SubscriptionMembership memberMembership = new SubscriptionMembership(alice, shared, MembershipRole.MEMBER);
        when(membershipRepository.findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
                        ALICE, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(ownerMembership, memberMembership));

        assertThat(service.findCurrent(ALICE))
                .containsExactly(
                        VisibleSubscriptionResponse.from(ownerMembership),
                        VisibleSubscriptionResponse.from(memberMembership));
    }

    @Test
    void findCancelled_shouldReturnEveryCancelledSubscriptionWithItsSavingsSoFar() {
        SubscriptionMembership ended = SubscriptionMembership.owner(alice, cancelled(1L, TODAY.minusMonths(1)));
        SubscriptionMembership endsLater = SubscriptionMembership.owner(alice, cancelled(2L, TODAY.plusDays(3)));
        stubAliceExists();
        when(subscriptionShares.load(ALICE, SubscriptionStatus.CANCELLED))
                .thenReturn(List.of(
                        new Share(ended, new BigDecimal("5.00")), new Share(endsLater, new BigDecimal("5.00"))));

        assertThat(service.findCancelled(ALICE))
                .extracting(cancelled -> cancelled.subscription().id(), CancelledSubscriptionResponse::saved)
                .containsExactly(tuple(1L, new BigDecimal("10.00")), tuple(2L, new BigDecimal("0.00")));
    }

    private static Subscription cancelled(long id, LocalDate endDate) {
        return aSubscription()
                .withId(id)
                .withNextPaymentDate(endDate)
                .cancelled()
                .build();
    }
}
