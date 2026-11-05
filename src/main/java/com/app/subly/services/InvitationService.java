package com.app.subly.services;

import com.app.subly.dto.invitation.InvitationResponse;
import com.app.subly.dto.invitation.InviteRequest;
import com.app.subly.dto.invitation.ReceivedInvitationResponse;
import com.app.subly.exceptions.invitation.AlreadyInvitedException;
import com.app.subly.exceptions.invitation.AlreadyMemberException;
import com.app.subly.exceptions.invitation.InvitedUserNotFoundException;
import com.app.subly.exceptions.invitation.SelfInvitationException;
import com.app.subly.models.User;
import com.app.subly.models.subscription.MembershipRole;
import com.app.subly.models.subscription.PendingInvitation;
import com.app.subly.models.subscription.Subscription;
import com.app.subly.repository.PendingInvitationRepository;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final SubscriptionAccess subscriptionAccess;
    private final SubscriptionMembershipRepository membershipRepository;
    private final PendingInvitationRepository invitationRepository;
    private final UserRepository userRepository;

    @Transactional
    public InvitationResponse invite(String ownerUsername, long subscriptionId, InviteRequest request) {
        Subscription subscription = subscriptionAccess.getOwnedSubscription(ownerUsername, subscriptionId);
        String usernameOrEmail = request.usernameOrEmail().strip();
        User invitedUser = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new InvitedUserNotFoundException(usernameOrEmail));
        if (invitedUser.getUsername().equals(ownerUsername)) {
            throw new SelfInvitationException();
        }
        if (membershipRepository.existsBySubscriptionIdAndUserId(subscriptionId, invitedUser.getId())) {
            throw new AlreadyMemberException(invitedUser.getUsername(), subscriptionId);
        }
        if (invitationRepository.existsBySubscriptionIdAndUserId(subscriptionId, invitedUser.getId())) {
            throw new AlreadyInvitedException(invitedUser.getUsername(), subscriptionId);
        }
        PendingInvitation invitation = invitationRepository.save(new PendingInvitation(invitedUser, subscription));
        return InvitationResponse.from(invitation);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> findPending(String ownerUsername, long subscriptionId) {
        subscriptionAccess.getOwnedSubscription(ownerUsername, subscriptionId);
        return invitationRepository.findBySubscriptionIdOrderByInvitedAt(subscriptionId).stream()
                .map(InvitationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivedInvitationResponse> findReceived(String username) {
        List<PendingInvitation> invitations = invitationRepository.findByUserUsernameOrderByInvitedAt(username);
        if (invitations.isEmpty()) {
            return List.of();
        }
        List<Long> subscriptionIds = invitations.stream()
                .map(invitation -> invitation.getSubscription().getId())
                .toList();
        Map<Long, String> ownerBySubscriptionId = membershipRepository
                .findBySubscriptionIdInAndRole(subscriptionIds, MembershipRole.OWNER)
                .stream()
                .collect(Collectors.toMap(
                        // same subscriptions the first query already loaded, so no extra query
                        owner -> owner.getSubscription().getId(),
                        owner -> owner.getUser().getUsername()));
        return invitations.stream()
                .map(invitation ->
                        ReceivedInvitationResponse.from(invitation, ownerBySubscriptionId.get(invitation.getSubscription().getId())))
                .toList();
    }

}
