package com.app.subly.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import com.app.subly.exception.invitation.AlreadyMemberException;
import com.app.subly.exception.invitation.InvitationCannotBeAcceptedException;
import com.app.subly.exception.invitation.InvitationNotFoundException;
import com.app.subly.model.TestUsers;
import com.app.subly.model.invitation.PendingInvitation;
import com.app.subly.model.subscription.*;
import com.app.subly.model.user.User;
import com.app.subly.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private SubscriptionAccess subscriptionAccess;

    @Mock
    private SubscriptionMembershipRepository membershipRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PendingInvitationRepository invitationRepository;

    @Mock
    private UserRepository userRepository;

    private InvitationService service;
    private Subscription subscription;
    private PendingInvitation invitation;

    @BeforeEach
    void setUp() {
        service = new InvitationService(
                subscriptionAccess, membershipRepository, invitationRepository, userRepository, subscriptionRepository);

        User bob = TestUsers.user("bob");
        bob.setId(2L);

        subscription =
                new Subscription(null, new BigDecimal("18.00"), BillingCycle.MONTHLY, LocalDate.of(2026, 12, 1), false);

        subscription.setId(10L);

        subscription.makeSharable(3);

        invitation = new PendingInvitation(bob, subscription);
    }

    @Test
    void acceptsInvitationAndCreatesMembership() {
        given(invitationRepository.getByIdAndUserUsername(5L, "bob")).willReturn(invitation);

        given(subscriptionRepository.findLockedById(10L)).willReturn(Optional.of(subscription));

        given(membershipRepository.countBySubscriptionId(10L)).willReturn(1L);

        service.accept("bob", 5L);

        ArgumentCaptor<SubscriptionMembership> captor = ArgumentCaptor.forClass(SubscriptionMembership.class);

        then(membershipRepository).should().save(captor.capture());

        SubscriptionMembership saved = captor.getValue();

        assertThat(saved.getRole()).isEqualTo(MembershipRole.MEMBER);
        assertThat(saved.getUser()).isSameAs(invitation.getUser());
        assertThat(saved.getSubscription()).isSameAs(subscription);

        then(invitationRepository).should().delete(invitation);
    }

    @Test
    void declinesInvitationWithoutCreatingMembership() {
        given(invitationRepository.getByIdAndUserUsername(5L, "bob")).willReturn(invitation);

        service.decline("bob", 5L);

        then(invitationRepository).should().delete(invitation);
        then(membershipRepository).shouldHaveNoInteractions();
    }

    @Test
    void cannotAcceptAnotherUsersInvitation() {
        given(invitationRepository.getByIdAndUserUsername(5L, "alice")).willThrow(new InvitationNotFoundException(5L));

        assertThatThrownBy(() -> service.accept("alice", 5L)).isInstanceOf(InvitationNotFoundException.class);

        then(membershipRepository).shouldHaveNoInteractions();
    }

    @Test
    void cannotDeclineAnotherUsersInvitation() {
        given(invitationRepository.getByIdAndUserUsername(5L, "alice")).willThrow(new InvitationNotFoundException(5L));

        assertThatThrownBy(() -> service.decline("alice", 5L)).isInstanceOf(InvitationNotFoundException.class);

        then(invitationRepository).should(never()).delete(any());
    }

    @Test
    void cannotAcceptFullSubscription() {
        given(invitationRepository.getByIdAndUserUsername(5L, "bob")).willReturn(invitation);

        given(subscriptionRepository.findLockedById(10L)).willReturn(Optional.of(subscription));

        given(membershipRepository.countBySubscriptionId(10L)).willReturn(3L);

        assertThatThrownBy(() -> service.accept("bob", 5L)).isInstanceOf(InvitationCannotBeAcceptedException.class);

        then(membershipRepository).should(never()).save(any());
    }

    @Test
    void cannotAcceptCancelledSubscription() {
        subscription.cancel(Instant.parse("2026-10-01T12:00:00Z"));

        given(invitationRepository.getByIdAndUserUsername(5L, "bob")).willReturn(invitation);

        given(subscriptionRepository.findLockedById(10L)).willReturn(Optional.of(subscription));

        assertThatThrownBy(() -> service.accept("bob", 5L)).isInstanceOf(InvitationCannotBeAcceptedException.class);

        then(membershipRepository).should(never()).save(any());
    }

    @Test
    void cannotAcceptNonSharableSubscription() {
        Subscription notSharable =
                new Subscription(null, new BigDecimal("18.00"), BillingCycle.MONTHLY, LocalDate.of(2026, 12, 1), false);

        given(invitationRepository.getByIdAndUserUsername(5L, "bob")).willReturn(invitation);

        given(subscriptionRepository.findLockedById(10L)).willReturn(Optional.of(notSharable));

        assertThatThrownBy(() -> service.accept("bob", 5L)).isInstanceOf(InvitationCannotBeAcceptedException.class);

        then(membershipRepository).should(never()).save(any());
    }

    @Test
    void cannotAcceptIfAlreadyMember() {
        given(invitationRepository.getByIdAndUserUsername(5L, "bob")).willReturn(invitation);

        given(subscriptionRepository.findLockedById(10L)).willReturn(Optional.of(subscription));

        given(membershipRepository.existsBySubscriptionIdAndUserId(10L, 2L)).willReturn(true);

        assertThatThrownBy(() -> service.accept("bob", 5L)).isInstanceOf(AlreadyMemberException.class);

        then(membershipRepository).should(never()).save(any());
    }
}
