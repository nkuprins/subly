package com.app.subly.service;

import com.app.subly.dto.invitation.InviteRequest;
import com.app.subly.dto.invitation.InviteResponse;
import com.app.subly.dto.invitation.ReceivedInvitationResponse;
import com.app.subly.exception.invitation.*;
import com.app.subly.model.invitation.PendingInvitation;
import com.app.subly.model.subscription.*;
import com.app.subly.model.user.User;
import com.app.subly.repository.PendingInvitationRepository;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final SubscriptionAccess subscriptionAccess;
    private final SubscriptionMembershipRepository membershipRepository;
    private final PendingInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public InviteResponse invite(String ownerUsername, long subscriptionId, InviteRequest request) {
        SubscriptionMembership owner = subscriptionAccess.getOwnerMembership(ownerUsername, subscriptionId);
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
        PendingInvitation invitation =
                invitationRepository.save(new PendingInvitation(invitedUser, owner.getSubscription(), owner.getUser()));
        return InviteResponse.from(invitation);
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> findPending(String ownerUsername, long subscriptionId) {
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
        Subscription subscription = subscriptionRepository
                .findLockedById(invitation.getSubscription().getId())
                .orElseThrow(() -> new InvitationCannotBeAcceptedException("Subscription no longer exists"));
        if (!subscription.isActive()) {
            throw new InvitationCannotBeAcceptedException("Subscription is not active");
        }
        if (!subscription.isSharable()) {
            throw new InvitationCannotBeAcceptedException("Subscription is not sharable");
        }
        Long subscriptionId = subscription.getId();
        requireNotMember(subscriptionId, invitation.getUser());
        long memberCount = membershipRepository.countBySubscriptionId(subscriptionId);
        if (subscription.isFull(memberCount)) {
            throw new InvitationCannotBeAcceptedException("Subscription has reached its participant limit");
        }
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

    private void requireNotMember(long subscriptionId, User user) {
        if (membershipRepository.existsBySubscriptionIdAndUserId(subscriptionId, user.getId())) {
            throw new AlreadyMemberException(user.getUsername(), subscriptionId);
        }
    }
}
