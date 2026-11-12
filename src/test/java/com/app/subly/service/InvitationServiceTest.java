package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.SUBSCRIPTION_ID;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.ALICE_ID;
import static com.app.subly.testdata.TestUsers.BOB;
import static com.app.subly.testdata.TestUsers.BOB_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.dto.invitation.InviteRequest;
import com.app.subly.entity.invitation.PendingInvitation;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.entity.user.User;
import com.app.subly.enums.MembershipRole;
import com.app.subly.exception.subscription.SubscriptionNotJoinableException;
import com.app.subly.repository.PendingInvitationRepository;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.service.access.SubscriptionAccess;
import com.app.subly.testdata.TestUsers;
import java.util.Optional;
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
    private static final int MAX_PARTICIPANTS = 3;

    @Mock
    SubscriptionMembershipRepository membershipRepository;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    PendingInvitationRepository invitationRepository;

    @Mock
    SubscriptionAccess subscriptionAccess;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    InvitationService service;

    @Captor
    ArgumentCaptor<SubscriptionMembership> membershipCaptor;

    @Captor
    ArgumentCaptor<PendingInvitation> invitationCaptor;

    final User alice = TestUsers.userWithId(ALICE, ALICE_ID);
    final User bob = TestUsers.userWithId(BOB, BOB_ID);
    final Subscription subscription = aSubscription()
            .withId(SUBSCRIPTION_ID)
            .withMaxParticipants(MAX_PARTICIPANTS)
            .build();
    final PendingInvitation invitation = new PendingInvitation(bob, subscription, alice);

    @Test
    void invite_shouldSaveInvitationWhileASeatIsFree() {
        stubOwnerMembership();
        when(membershipRepository.countBySubscriptionId(SUBSCRIPTION_ID)).thenReturn(MAX_PARTICIPANTS - 1L);
        when(userRepository.findByUsername(BOB)).thenReturn(Optional.of(bob));

        service.invite(ALICE, SUBSCRIPTION_ID, new InviteRequest(BOB));

        verify(invitationRepository).save(invitationCaptor.capture());
        PendingInvitation saved = invitationCaptor.getValue();
        assertThat(saved.getUser()).isSameAs(bob);
        assertThat(saved.getSubscription()).isSameAs(subscription);
        assertThat(saved.getInvitedBy()).isSameAs(alice);
    }

    @Test
    void invite_shouldRejectFullSubscription() {
        stubOwnerMembership();
        when(membershipRepository.countBySubscriptionId(SUBSCRIPTION_ID)).thenReturn((long) MAX_PARTICIPANTS);
        InviteRequest request = new InviteRequest(BOB);

        assertThatThrownBy(() -> service.invite(ALICE, SUBSCRIPTION_ID, request))
                .isInstanceOf(SubscriptionNotJoinableException.class);
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void accept_shouldCreateMembershipAndDeleteInvitation() {
        stubInvitation();
        when(membershipRepository.countBySubscriptionId(SUBSCRIPTION_ID)).thenReturn(MAX_PARTICIPANTS - 1L);

        service.accept(BOB, INVITATION_ID);

        verify(membershipRepository).save(membershipCaptor.capture());
        SubscriptionMembership saved = membershipCaptor.getValue();
        assertThat(saved.getRole()).isEqualTo(MembershipRole.MEMBER);
        assertThat(saved.getUser()).isSameAs(bob);
        assertThat(saved.getSubscription()).isSameAs(subscription);
        verify(invitationRepository).delete(invitation);
    }

    @Test
    void accept_shouldRejectFullSubscription() {
        stubInvitation();
        when(membershipRepository.countBySubscriptionId(SUBSCRIPTION_ID)).thenReturn((long) MAX_PARTICIPANTS);

        assertThatThrownBy(() -> service.accept(BOB, INVITATION_ID))
                .isInstanceOf(SubscriptionNotJoinableException.class);
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void decline_shouldDeleteInvitationWithoutCreatingMembership() {
        when(invitationRepository.getInvitation(INVITATION_ID, BOB)).thenReturn(invitation);

        service.decline(BOB, INVITATION_ID);

        verify(invitationRepository).delete(invitation);
        verifyNoInteractions(membershipRepository);
    }

    private void stubOwnerMembership() {
        when(subscriptionAccess.getOwnerMembership(ALICE, SUBSCRIPTION_ID))
                .thenReturn(SubscriptionMembership.owner(alice, subscription));
    }

    private void stubInvitation() {
        when(invitationRepository.getInvitation(INVITATION_ID, BOB)).thenReturn(invitation);
        when(subscriptionRepository.findLockedById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
    }
}
