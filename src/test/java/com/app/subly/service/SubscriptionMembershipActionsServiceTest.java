package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.testdata.TestUsers;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SubscriptionMembershipActionsServiceTest extends AbstractSubscriptionServiceTest {

    private static final long MEMBERSHIP_ID = 45L;

    @Test
    void leave_shouldRemoveMemberWithoutDeletingSubscription() {
        Subscription subscription = aSubscription().withId(SUBSCRIPTION_ID).build();

        SubscriptionMembership member =
                new SubscriptionMembership(TestUsers.userWithId(BOB, 2L), subscription, MembershipRole.MEMBER);

        when(membershipRepository.findBySubscriptionIdAndUserUsername(SUBSCRIPTION_ID, BOB))
                .thenReturn(Optional.of(member));

        service.leave(BOB, SUBSCRIPTION_ID);

        verify(membershipRepository).delete(member);
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void removeMember_shouldAllowOwnerToKickMember() {
        Subscription subscription = aSubscription().withId(SUBSCRIPTION_ID).build();
        stubOwnedSubscription(subscription);

        SubscriptionMembership member =
                new SubscriptionMembership(TestUsers.userWithId(BOB, 2L), subscription, MembershipRole.MEMBER);

        member.setId(MEMBERSHIP_ID);

        when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(member));

        service.removeMember(ALICE, SUBSCRIPTION_ID, MEMBERSHIP_ID);

        verify(membershipRepository).delete(member);
        verifyNoInteractions(subscriptionRepository);
    }
}
