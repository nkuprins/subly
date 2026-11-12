package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.SUBSCRIPTION_ID;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static com.app.subly.testdata.TestUsers.CAROL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.SubscriptionMemberResponse;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.entity.user.User;
import com.app.subly.enums.MembershipRole;
import com.app.subly.exception.subscription.MembershipActionForbiddenException;
import com.app.subly.exception.subscription.SubscriptionMemberNotFoundException;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.service.access.SubscriptionAccess;
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

    private static final long UNKNOWN_MEMBERSHIP_ID = 99L;

    @Mock
    SubscriptionAccess subscriptionAccess;

    @Mock
    SubscriptionMembershipRepository membershipRepository;

    @InjectMocks
    MembershipService service;

    final User alice = TestUsers.user(ALICE);
    final Subscription subscription =
            aSubscription().withId(SUBSCRIPTION_ID).withPrice("10.00").build();
    final SubscriptionMembership owner = withId(1L, SubscriptionMembership.owner(alice, subscription));
    final SubscriptionMembership bobMember =
            withId(2L, SubscriptionMembership.member(TestUsers.user(BOB), subscription));
    final SubscriptionMembership carolMember =
            withId(3L, SubscriptionMembership.member(TestUsers.user(CAROL), subscription));

    @Test
    void findMembers_shouldReturnEveryMemberWithTheirShareOfThePrice() {
        stubMembership(ALICE, owner);
        stubMembers();

        assertThat(service.findMembers(ALICE, SUBSCRIPTION_ID).members())
                .containsExactly(
                        new SubscriptionMemberResponse(1L, ALICE, MembershipRole.OWNER, new BigDecimal("3.34")),
                        new SubscriptionMemberResponse(2L, BOB, MembershipRole.MEMBER, new BigDecimal("3.33")),
                        new SubscriptionMemberResponse(3L, CAROL, MembershipRole.MEMBER, new BigDecimal("3.33")));
    }

    @Test
    void findMembers_shouldBeFullWhenEverySeatIsTaken() {
        stubMembership(
                ALICE,
                SubscriptionMembership.owner(
                        alice, aSubscription().withMaxParticipants(3).build()));
        stubMembers();

        assertThat(service.findMembers(ALICE, SUBSCRIPTION_ID).full()).isTrue();
    }

    @Test
    void findMembers_shouldNotBeFullWhileASeatIsFree() {
        stubMembership(
                ALICE,
                SubscriptionMembership.owner(
                        alice, aSubscription().withMaxParticipants(4).build()));
        stubMembers();

        assertThat(service.findMembers(ALICE, SUBSCRIPTION_ID).full()).isFalse();
    }

    @Test
    void leave_shouldRemoveTheMembership() {
        stubMembership(BOB, bobMember);

        service.leave(BOB, SUBSCRIPTION_ID);

        verify(membershipRepository).delete(bobMember);
    }

    @Test
    void leave_shouldRejectOwner() {
        stubMembership(ALICE, owner);

        assertThatThrownBy(() -> service.leave(ALICE, SUBSCRIPTION_ID))
                .isInstanceOf(MembershipActionForbiddenException.class);
        verify(membershipRepository, never()).delete(any());
    }

    @Test
    void removeMember_shouldAllowOwnerToKickMember() {
        when(membershipRepository.findByIdAndSubscriptionId(bobMember.getId(), SUBSCRIPTION_ID))
                .thenReturn(Optional.of(bobMember));

        service.removeMember(ALICE, SUBSCRIPTION_ID, bobMember.getId());

        verify(subscriptionAccess).getOwnedSubscription(ALICE, SUBSCRIPTION_ID);
        verify(membershipRepository).delete(bobMember);
    }

    @Test
    void removeMember_shouldRejectOwner() {
        Long ownerId = owner.getId();
        when(membershipRepository.findByIdAndSubscriptionId(ownerId, SUBSCRIPTION_ID))
                .thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.removeMember(ALICE, SUBSCRIPTION_ID, ownerId))
                .isInstanceOf(MembershipActionForbiddenException.class);
        verify(membershipRepository, never()).delete(any());
    }

    @Test
    void removeMember_shouldRejectUnknownMember() {
        when(membershipRepository.findByIdAndSubscriptionId(UNKNOWN_MEMBERSHIP_ID, SUBSCRIPTION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeMember(ALICE, SUBSCRIPTION_ID, UNKNOWN_MEMBERSHIP_ID))
                .isInstanceOf(SubscriptionMemberNotFoundException.class);
        verify(membershipRepository, never()).delete(any());
    }

    private void stubMembership(String username, SubscriptionMembership membership) {
        when(subscriptionAccess.getMembership(username, SUBSCRIPTION_ID)).thenReturn(membership);
    }

    private void stubMembers() {
        when(membershipRepository.findBySubscriptionIdOrderByIdAsc(SUBSCRIPTION_ID))
                .thenReturn(List.of(owner, bobMember, carolMember));
    }

    private static SubscriptionMembership withId(long id, SubscriptionMembership membership) {
        membership.setId(id);
        return membership;
    }
}
