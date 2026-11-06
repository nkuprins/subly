package com.app.subly.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import com.app.subly.exception.invitation.AlreadyMemberException;
import com.app.subly.exception.invitation.InvitationCannotBeAcceptedException;
import com.app.subly.exception.invitation.InvitationNotFoundException;
import com.app.subly.model.User;
import com.app.subly.model.subscription.*;
import com.app.subly.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");

        subscription =
                new Subscription(null, new BigDecimal("18.00"), BillingCycle.MONTHLY, LocalDate.of(2026, 12, 1), false);

        ReflectionTestUtils.setField(subscription, "id", 10L);

        subscription.setSharable(true);
        subscription.setMaxParticipants(3);

        invitation = new PendingInvitation(bob, subscription);
    }

    @Test
    void acceptsInvitationAndCreatesMembership() {
        given(invitationRepository.findByIdAndUserUsername(5L, "bob")).willReturn(Optional.of(invitation));

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
        given(invitationRepository.findByIdAndUserUsername(5L, "bob")).willReturn(Optional.of(invitation));

        service.decline("bob", 5L);

        then(invitationRepository).should().delete(invitation);
        then(membershipRepository).shouldHaveNoInteractions();
    }

    @Test
    void cannotAcceptAnotherUsersInvitation() {
        assertThatThrownBy(() -> service.accept("alice", 5L)).isInstanceOf(InvitationNotFoundException.class);

        then(membershipRepository).shouldHaveNoInteractions();
    }

    @Test
    void cannotAcceptFullSubscription() {
        given(invitationRepository.findByIdAndUserUsername(5L, "bob")).willReturn(Optional.of(invitation));

        given(subscriptionRepository.findLockedById(10L)).willReturn(Optional.of(subscription));

        given(membershipRepository.countBySubscriptionId(10L)).willReturn(3L);

        assertThatThrownBy(() -> service.accept("bob", 5L)).isInstanceOf(InvitationCannotBeAcceptedException.class);

        then(membershipRepository).should(never()).save(any());
    }

    @Test
    void cannotAcceptCancelledSubscription() {
        subscription.setStatus(SubscriptionStatus.CANCELLED);

        given(invitationRepository.findByIdAndUserUsername(5L, "bob")).willReturn(Optional.of(invitation));

        given(subscriptionRepository.findLockedById(10L)).willReturn(Optional.of(subscription));

        assertThatThrownBy(() -> service.accept("bob", 5L)).isInstanceOf(InvitationCannotBeAcceptedException.class);

        then(membershipRepository).should(never()).save(any());
    }

    @Test
    void cannotAcceptNonSharableSubscription() {
        subscription.setSharable(false);

        given(invitationRepository.findByIdAndUserUsername(5L, "bob")).willReturn(Optional.of(invitation));

        given(subscriptionRepository.findLockedById(10L)).willReturn(Optional.of(subscription));

        assertThatThrownBy(() -> service.accept("bob", 5L)).isInstanceOf(InvitationCannotBeAcceptedException.class);

        then(membershipRepository).should(never()).save(any());
    }

    @Test
    void cannotAcceptIfAlreadyMember() {
        given(invitationRepository.findByIdAndUserUsername(5L, "bob")).willReturn(Optional.of(invitation));

        given(subscriptionRepository.findLockedById(10L)).willReturn(Optional.of(subscription));

        given(membershipRepository.existsBySubscriptionIdAndUserId(10L, 2L)).willReturn(true);

        assertThatThrownBy(() -> service.accept("bob", 5L)).isInstanceOf(AlreadyMemberException.class);

        then(membershipRepository).should(never()).save(any());
    }
}
