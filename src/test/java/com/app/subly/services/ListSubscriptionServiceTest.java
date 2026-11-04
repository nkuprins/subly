package com.app.subly.services;

import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.models.subscription.MembershipRole;
import com.app.subly.models.subscription.Subscription;
import com.app.subly.models.subscription.SubscriptionMembership;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class ListSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void returnsResponseForEachOwnedSubscription() {
        Subscription subscription = subscription(customService());
        given(membershipRepository.findByUserUsernameAndRoleOrderBySubscriptionNextPaymentDate(
                OWNER, MembershipRole.OWNER)).willReturn(List.of(SubscriptionMembership.owner(owner, subscription)));

        assertThat(subscriptionService.findOwnedBy(OWNER)).containsExactly(SubscriptionResponse.from(subscription));
    }
}
