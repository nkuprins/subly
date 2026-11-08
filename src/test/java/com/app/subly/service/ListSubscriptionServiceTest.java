package com.app.subly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.app.subly.dto.subscription.VisibleSubscriptionResponse;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void returnsVisibleSubscriptionsWithMembershipRoles() {
        Subscription owned = subscription(customProvider());
        Subscription shared = subscription(catalogProvider());

        SubscriptionMembership ownerMembership = SubscriptionMembership.owner(owner, owned);

        SubscriptionMembership memberMembership = new SubscriptionMembership(owner, shared, MembershipRole.MEMBER);

        given(membershipRepository.findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
                        OWNER, SubscriptionStatus.ACTIVE))
                .willReturn(List.of(ownerMembership, memberMembership));

        assertThat(subscriptionService.findVisibleTo(OWNER))
                .containsExactly(
                        VisibleSubscriptionResponse.from(ownerMembership),
                        VisibleSubscriptionResponse.from(memberMembership));
    }
}
