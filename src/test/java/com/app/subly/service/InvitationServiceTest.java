package com.app.subly.service;

import static com.app.subly.testdata.TestProviders.CATALOG_PROVIDER_NAME;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_PRICE;
import static com.app.subly.testdata.TestSubscriptions.SUBSCRIPTION_ID;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.ALICE_ID;
import static com.app.subly.testdata.TestUsers.BOB;
import static com.app.subly.testdata.TestUsers.BOB_ID;
import static com.app.subly.testdata.TestUsers.email;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.dto.invitation.InviteRequest;
import com.app.subly.dto.invitation.InviteResponse;
import com.app.subly.dto.invitation.ReceivedInvitationResponse;
import com.app.subly.entity.invitation.PendingInvitation;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.entity.user.User;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import com.app.subly.enums.MembershipRole;
import com.app.subly.exception.invitation.AlreadyInvitedException;
import com.app.subly.exception.invitation.AlreadyMemberException;
import com.app.subly.exception.invitation.InvitedUserNotFoundException;
import com.app.subly.exception.invitation.SelfInvitationException;
import com.app.subly.exception.subscription.SubscriptionNotJoinableException;
import com.app.subly.repository.PendingInvitationRepository;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.service.access.SubscriptionAccess;
import com.app.subly.testdata.TestUsers;
import java.util.List;
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
    private static final String UNKNOWN_USER = "nobody";

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
        stubMemberCount(MAX_PARTICIPANTS - 1L);
        stubBobExists();

        service.invite(ALICE, SUBSCRIPTION_ID, new InviteRequest(BOB));

        verify(invitationRepository).save(invitationCaptor.capture());
        PendingInvitation saved = invitationCaptor.getValue();
        assertThat(saved.getUser()).isSameAs(bob);
        assertThat(saved.getSubscription()).isSameAs(subscription);
        assertThat(saved.getInvitedBy()).isSameAs(alice);
    }

    @Test
    void invite_shouldFindInvitedUserByEmail() {
        stubOwnerMembership();
        stubMemberCount(MAX_PARTICIPANTS - 1L);
        when(userRepository.findByUsername(email(BOB))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email(BOB))).thenReturn(Optional.of(bob));

        service.invite(ALICE, SUBSCRIPTION_ID, new InviteRequest(email(BOB)));

        verify(invitationRepository).save(invitationCaptor.capture());
        assertThat(invitationCaptor.getValue().getUser()).isSameAs(bob);
    }

    @Test
    void invite_shouldRejectFullSubscription() {
        stubOwnerMembership();
        stubMemberCount(MAX_PARTICIPANTS);

        assertInviteRejected(SubscriptionNotJoinableException.class, BOB);
    }

    @Test
    void invite_shouldRejectUnknownUser() {
        stubOwnerMembership();
        stubMemberCount(MAX_PARTICIPANTS - 1L);
        when(userRepository.findByUsername(UNKNOWN_USER)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(UNKNOWN_USER)).thenReturn(Optional.empty());

        assertInviteRejected(InvitedUserNotFoundException.class, UNKNOWN_USER);
    }

    @Test
    void invite_shouldRejectSelfInvitation() {
        stubOwnerMembership();
        stubMemberCount(MAX_PARTICIPANTS - 1L);
        when(userRepository.findByUsername(ALICE)).thenReturn(Optional.of(alice));

        assertInviteRejected(SelfInvitationException.class, ALICE);
    }

    @Test
    void invite_shouldRejectExistingMember() {
        stubOwnerMembership();
        stubMemberCount(MAX_PARTICIPANTS - 1L);
        stubBobExists();
        when(membershipRepository.existsBySubscriptionIdAndUserId(SUBSCRIPTION_ID, BOB_ID))
                .thenReturn(true);

        assertInviteRejected(AlreadyMemberException.class, BOB);
    }

    @Test
    void invite_shouldRejectAlreadyInvitedUser() {
        stubOwnerMembership();
        stubMemberCount(MAX_PARTICIPANTS - 1L);
        stubBobExists();
        when(invitationRepository.existsBySubscriptionIdAndUserId(SUBSCRIPTION_ID, BOB_ID))
                .thenReturn(true);

        assertInviteRejected(AlreadyInvitedException.class, BOB);
    }

    @Test
    void findPending_shouldReturnInvitationsOfOwnedSubscription() {
        when(invitationRepository.findBySubscriptionIdOrderByInvitedAtAsc(SUBSCRIPTION_ID))
                .thenReturn(List.of(invitation));

        assertThat(service.findPending(ALICE, SUBSCRIPTION_ID))
                .extracting(InviteResponse::username)
                .containsExactly(BOB);
    }

    @Test
    void findReceived_shouldReturnInvitationsOfTheUser() {
        invitation.setId(INVITATION_ID);
        when(invitationRepository.findByUserUsernameOrderByInvitedAtAsc(BOB)).thenReturn(List.of(invitation));

        assertThat(service.findReceived(BOB))
                .containsExactly(new ReceivedInvitationResponse(
                        INVITATION_ID,
                        CATALOG_PROVIDER_NAME,
                        ALICE,
                        DEFAULT_PRICE,
                        Currency.EUR,
                        BillingCycle.MONTHLY));
    }

    @Test
    void accept_shouldCreateMembershipAndDeleteInvitation() {
        stubInvitation();
        stubLockedSubscription();
        stubMemberCount(MAX_PARTICIPANTS - 1L);

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
        stubLockedSubscription();
        stubMemberCount(MAX_PARTICIPANTS);

        assertThatThrownBy(() -> service.accept(BOB, INVITATION_ID))
                .isInstanceOf(SubscriptionNotJoinableException.class);
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void accept_shouldRejectDeletedSubscription() {
        stubInvitation();
        when(subscriptionRepository.findLockedById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(BOB, INVITATION_ID))
                .isInstanceOf(SubscriptionNotJoinableException.class);
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void decline_shouldDeleteInvitationWithoutCreatingMembership() {
        stubInvitation();

        service.decline(BOB, INVITATION_ID);

        verify(invitationRepository).delete(invitation);
        verifyNoInteractions(membershipRepository);
    }

    @Test
    void deleteAllInvitationsFor_shouldDeleteInvitationsOfTheUser() {
        service.deleteAllInvitationsFor(BOB_ID);

        verify(invitationRepository).deleteByUserId(BOB_ID);
        verify(invitationRepository).flush();
    }

    private void assertInviteRejected(Class<? extends RuntimeException> exception, String usernameOrEmail) {
        InviteRequest request = new InviteRequest(usernameOrEmail);

        assertThatThrownBy(() -> service.invite(ALICE, SUBSCRIPTION_ID, request))
                .isInstanceOf(exception);
        verify(invitationRepository, never()).save(any());
    }

    private void stubOwnerMembership() {
        when(subscriptionAccess.getOwnerMembership(ALICE, SUBSCRIPTION_ID))
                .thenReturn(SubscriptionMembership.owner(alice, subscription));
    }

    private void stubMemberCount(long memberCount) {
        when(membershipRepository.countBySubscriptionId(SUBSCRIPTION_ID)).thenReturn(memberCount);
    }

    private void stubBobExists() {
        when(userRepository.findByUsername(BOB)).thenReturn(Optional.of(bob));
    }

    private void stubInvitation() {
        when(invitationRepository.getInvitation(INVITATION_ID, BOB)).thenReturn(invitation);
    }

    private void stubLockedSubscription() {
        when(subscriptionRepository.findLockedById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
    }
}
