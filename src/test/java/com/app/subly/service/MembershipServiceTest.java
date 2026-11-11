package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static com.app.subly.testdata.TestUsers.CAROL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.SubscriptionMemberResponse;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.testdata.TestUsers;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    private static final long SUBSCRIPTION_ID = 7L;

    @Mock
    SubscriptionAccess subscriptionAccess;

    @Mock
    SubscriptionMembershipRepository membershipRepository;

    @InjectMocks
    MembershipService service;

    final Subscription subscription =
            aSubscription().withId(SUBSCRIPTION_ID).withPrice("10.00").build();
    final SubscriptionMembership owner = withId(1L, SubscriptionMembership.owner(TestUsers.user(ALICE), subscription));
    final SubscriptionMembership bob = withId(2L, SubscriptionMembership.member(TestUsers.user(BOB), subscription));
    final SubscriptionMembership carol = withId(3L, SubscriptionMembership.member(TestUsers.user(CAROL), subscription));

    @Test
    void findMembers_shouldReturnEveryMemberWithTheirShareOfThePrice() {
        when(subscriptionAccess.getMembership(ALICE, SUBSCRIPTION_ID)).thenReturn(owner);
        stubMembers();

        assertThat(service.findMembers(ALICE, SUBSCRIPTION_ID).members())
                .containsExactly(
                        new SubscriptionMemberResponse(1L, ALICE, MembershipRole.OWNER, new BigDecimal("3.34")),
                        new SubscriptionMemberResponse(2L, BOB, MembershipRole.MEMBER, new BigDecimal("3.33")),
                        new SubscriptionMemberResponse(3L, CAROL, MembershipRole.MEMBER, new BigDecimal("3.33")));
    }

    @Test
    void findMembers_shouldReturnEveryMemberToAMember() {
        when(subscriptionAccess.getMembership(BOB, SUBSCRIPTION_ID)).thenReturn(bob);
        stubMembers();

        assertThat(service.findMembers(BOB, SUBSCRIPTION_ID).members())
                .extracting(SubscriptionMemberResponse::username)
                .containsExactly(ALICE, BOB, CAROL);
    }

    @Test
    void findMembers_shouldBeFullWhenEverySeatIsTaken() {
        stubOwnerOf(aSubscription().withMaxParticipants(3).build());
        stubMembers();

        assertThat(service.findMembers(ALICE, SUBSCRIPTION_ID).full()).isTrue();
    }

    @Test
    void findMembers_shouldNotBeFullWhileASeatIsFree() {
        stubOwnerOf(aSubscription().withMaxParticipants(4).build());
        stubMembers();

        assertThat(service.findMembers(ALICE, SUBSCRIPTION_ID).full()).isFalse();
    }

    @Test
    void findMembers_shouldBeFullWhenCancelled() {
        stubOwnerOf(aSubscription().withMaxParticipants(4).cancelled().build());
        stubMembers();

        assertThat(service.findMembers(ALICE, SUBSCRIPTION_ID).full()).isTrue();
    }

    @Test
    void findMembers_shouldRejectUserOutsideTheSubscription() {
        when(subscriptionAccess.getMembership(BOB, SUBSCRIPTION_ID))
                .thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

        assertThatThrownBy(() -> service.findMembers(BOB, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionNotFoundException.class);
        verifyNoInteractions(membershipRepository);
    }

    @Test
    void leave_shouldRemoveTheMembership() {
        when(subscriptionAccess.getMembership(BOB, SUBSCRIPTION_ID)).thenReturn(bob);

        service.leave(BOB, SUBSCRIPTION_ID);

        verify(membershipRepository).delete(bob);
    }

    @Test
    void removeMember_shouldAllowOwnerToKickMember() {
        when(subscriptionAccess.getOwnedSubscription(ALICE, SUBSCRIPTION_ID)).thenReturn(subscription);
        when(membershipRepository.findByIdAndSubscriptionId(bob.getId(), SUBSCRIPTION_ID))
                .thenReturn(Optional.of(bob));

        service.removeMember(ALICE, SUBSCRIPTION_ID, bob.getId());

        verify(membershipRepository).delete(bob);
    }

    private void stubOwnerOf(Subscription ownedSubscription) {
        when(subscriptionAccess.getMembership(ALICE, SUBSCRIPTION_ID))
                .thenReturn(SubscriptionMembership.owner(TestUsers.user(ALICE), ownedSubscription));
    }

    private void stubMembers() {
        when(membershipRepository.findBySubscriptionIdOrderByIdAsc(SUBSCRIPTION_ID))
                .thenReturn(List.of(owner, bob, carol));
    }

    private static SubscriptionMembership withId(long id, SubscriptionMembership membership) {
        membership.setId(id);
        return membership;
    }
}
