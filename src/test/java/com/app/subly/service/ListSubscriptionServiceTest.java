package com.app.subly.service;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.VisibleSubscriptionResponse;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.testdata.TestClock;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void findVisibleTo_shouldReturnSubscriptionsWithMembershipRoles() {
        Subscription owned = subscription(customProvider());
        Subscription shared = subscription(catalogProvider());
        SubscriptionMembership ownerMembership = SubscriptionMembership.owner(alice, owned);
        SubscriptionMembership memberMembership = new SubscriptionMembership(alice, shared, MembershipRole.MEMBER);
        when(membershipRepository.findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
                        ALICE, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(ownerMembership, memberMembership));

        assertThat(service.findVisibleTo(ALICE))
                .containsExactly(
                        VisibleSubscriptionResponse.from(ownerMembership),
                        VisibleSubscriptionResponse.from(memberMembership));
    }

    @Test
    void findCancelledVisibleTo_shouldReturnCancelledSubscriptionsWithMembershipRoles() {
        Subscription cancelled = subscription(customProvider());
        cancelled.cancel(TestClock.NOW);
        SubscriptionMembership ownerMembership = SubscriptionMembership.owner(alice, cancelled);
        when(membershipRepository.findByUserUsernameAndSubscriptionStatusOrderBySubscriptionCancelledAtDesc(
                        ALICE, SubscriptionStatus.CANCELLED))
                .thenReturn(List.of(ownerMembership));

        assertThat(service.findCancelledVisibleTo(ALICE)).singleElement().satisfies(response -> {
            assertThat(response.role()).isEqualTo(MembershipRole.OWNER);
            assertThat(response.subscription().status()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(response.subscription().cancelledAt()).isEqualTo(TestClock.NOW);
        });
    }
}
