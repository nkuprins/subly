package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.exception.invitation.AlreadyMemberException;
import com.app.subly.exception.invitation.InvitationCannotBeAcceptedException;
import com.app.subly.exception.invitation.InvitationNotFoundException;
import com.app.subly.model.invitation.PendingInvitation;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.user.User;
import com.app.subly.repository.PendingInvitationRepository;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestUsers;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    private static final long INVITATION_ID = 5L;
    private static final long SUBSCRIPTION_ID = 10L;
    private static final long INVITEE_ID = 2L;
    private static final int MAX_PARTICIPANTS = 3;

    @Mock
    SubscriptionAccess subscriptionAccess;

    @Mock
    SubscriptionMembershipRepository membershipRepository;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    PendingInvitationRepository invitationRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    InvitationService service;

    @Captor
    ArgumentCaptor<SubscriptionMembership> membershipCaptor;

    Subscription subscription;
    PendingInvitation invitation;

    @BeforeEach
    void setUp() {
        User invitee = TestUsers.user(ALICE);
        invitee.setId(INVITEE_ID);
        subscription = aSubscription()
                .withId(SUBSCRIPTION_ID)
                .withMaxParticipants(MAX_PARTICIPANTS)
                .build();
        invitation = new PendingInvitation(invitee, subscription);
    }

    @Test
    void accept_shouldCreateMembershipAndDeleteInvitation() {
        stubInvitationTo(subscription);
        when(membershipRepository.countBySubscriptionId(SUBSCRIPTION_ID)).thenReturn(1L);

        service.accept(ALICE, INVITATION_ID);

        verify(membershipRepository).save(membershipCaptor.capture());
        SubscriptionMembership saved = membershipCaptor.getValue();
        assertThat(saved.getRole()).isEqualTo(MembershipRole.MEMBER);
        assertThat(saved.getUser()).isSameAs(invitation.getUser());
        assertThat(saved.getSubscription()).isSameAs(subscription);
        verify(invitationRepository).delete(invitation);
    }

    @Test
    void decline_shouldDeleteInvitationWithoutCreatingMembership() {
        when(invitationRepository.getByIdAndUserUsername(INVITATION_ID, ALICE)).thenReturn(invitation);

        service.decline(ALICE, INVITATION_ID);

        verify(invitationRepository).delete(invitation);
        verifyNoInteractions(membershipRepository);
    }

    @Test
    void accept_shouldRejectInvitationOfAnotherUser() {
        when(invitationRepository.getByIdAndUserUsername(INVITATION_ID, BOB))
                .thenThrow(new InvitationNotFoundException(INVITATION_ID));

        assertThatThrownBy(() -> service.accept(BOB, INVITATION_ID)).isInstanceOf(InvitationNotFoundException.class);
        verifyNoInteractions(membershipRepository);
    }

    @Test
    void decline_shouldRejectInvitationOfAnotherUser() {
        when(invitationRepository.getByIdAndUserUsername(INVITATION_ID, BOB))
                .thenThrow(new InvitationNotFoundException(INVITATION_ID));

        assertThatThrownBy(() -> service.decline(BOB, INVITATION_ID)).isInstanceOf(InvitationNotFoundException.class);
        verify(invitationRepository, never()).delete(any());
    }

    @Test
    void accept_shouldRejectFullSubscription() {
        stubInvitationTo(subscription);
        when(membershipRepository.countBySubscriptionId(SUBSCRIPTION_ID)).thenReturn((long) MAX_PARTICIPANTS);

        assertAcceptRejectedWith(InvitationCannotBeAcceptedException.class);
    }

    @Test
    void accept_shouldRejectCancelledSubscription() {
        subscription.cancel(Instant.parse("2026-10-01T12:00:00Z"));
        stubInvitationTo(subscription);

        assertAcceptRejectedWith(InvitationCannotBeAcceptedException.class);
    }

    @Test
    void accept_shouldRejectNonSharableSubscription() {
        stubInvitationTo(aSubscription().build());

        assertAcceptRejectedWith(InvitationCannotBeAcceptedException.class);
    }

    @Test
    void accept_shouldRejectExistingMember() {
        stubInvitationTo(subscription);
        when(membershipRepository.existsBySubscriptionIdAndUserId(SUBSCRIPTION_ID, INVITEE_ID))
                .thenReturn(true);

        assertAcceptRejectedWith(AlreadyMemberException.class);
    }

    private void stubInvitationTo(Subscription lockedSubscription) {
        when(invitationRepository.getByIdAndUserUsername(INVITATION_ID, ALICE)).thenReturn(invitation);
        when(subscriptionRepository.findLockedById(SUBSCRIPTION_ID)).thenReturn(Optional.of(lockedSubscription));
    }

    private void assertAcceptRejectedWith(Class<? extends RuntimeException> exception) {
        assertThatThrownBy(() -> service.accept(ALICE, INVITATION_ID)).isInstanceOf(exception);
        verify(membershipRepository, never()).save(any());
    }
}
