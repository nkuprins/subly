package com.app.subly.service;

import com.app.subly.dto.invitation.InviteRequest;
import com.app.subly.dto.invitation.InviteResponse;
import com.app.subly.dto.invitation.ReceivedInvitationResponse;
import com.app.subly.entity.invitation.PendingInvitation;
import com.app.subly.entity.subscription.*;
import com.app.subly.entity.user.User;
import com.app.subly.exception.invitation.*;
import com.app.subly.exception.subscription.SubscriptionNotJoinableException;
import com.app.subly.repository.PendingInvitationRepository;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final SubscriptionAccess subscriptionAccess;
    private final SubscriptionMembershipRepository membershipRepository;
    private final PendingInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public void invite(String ownerUsername, long subscriptionId, InviteRequest request) {
        SubscriptionMembership owner = subscriptionAccess.getOwnerMembership(ownerUsername, subscriptionId);
        requireJoinable(owner.getSubscription());
        String usernameOrEmail = request.usernameOrEmail().strip();
        User invitedUser = userRepository
                .findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new InvitedUserNotFoundException(usernameOrEmail));
        if (invitedUser.getUsername().equals(ownerUsername)) {
            throw new SelfInvitationException();
        }
        requireNotMember(subscriptionId, invitedUser);
        if (invitationRepository.existsBySubscriptionIdAndUserId(subscriptionId, invitedUser.getId())) {
            throw new AlreadyInvitedException(invitedUser.getUsername(), subscriptionId);
        }
        invitationRepository.save(new PendingInvitation(invitedUser, owner.getSubscription(), owner.getUser()));
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> findPending(String ownerUsername, long subscriptionId) {
        log.debug("Getting pending for username={} subscriptionId={}", ownerUsername, subscriptionId);
        subscriptionAccess.getOwnedSubscription(ownerUsername, subscriptionId);
        return invitationRepository.findBySubscriptionIdOrderByInvitedAt(subscriptionId).stream()
                .map(InviteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivedInvitationResponse> findReceived(String username) {
        return invitationRepository.findByUserUsernameOrderByInvitedAt(username).stream()
                .map(ReceivedInvitationResponse::from)
                .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void accept(String username, long invitationId) {
        PendingInvitation invitation = invitationRepository.getByIdAndUserUsername(invitationId, username);
        long subscriptionId = invitation.getSubscription().getId();
        Subscription subscription = subscriptionRepository
                .findLockedById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotJoinableException(subscriptionId));
        requireJoinable(subscription);
        SubscriptionMembership membership = SubscriptionMembership.member(invitation.getUser(), subscription);
        membershipRepository.save(membership);
        invitationRepository.delete(invitation);
    }

    @Transactional
    public void decline(String username, long invitationId) {
        PendingInvitation invitation = invitationRepository.getByIdAndUserUsername(invitationId, username);
        invitationRepository.delete(invitation);
    }

    @Transactional
    public void deleteInvitationFor(Long userId) {
        invitationRepository.deleteByUser_Id(userId);
        invitationRepository.flush();
    }

    private void requireJoinable(Subscription subscription) {
        if (subscription.isClosedToNewMembers(membershipRepository.countBySubscriptionId(subscription.getId()))) {
            throw new SubscriptionNotJoinableException(subscription.getId());
        }
    }

    private void requireNotMember(long subscriptionId, User user) {
        if (membershipRepository.existsBySubscriptionIdAndUserId(subscriptionId, user.getId())) {
            throw new AlreadyMemberException(user.getUsername(), subscriptionId);
        }
    }
}
