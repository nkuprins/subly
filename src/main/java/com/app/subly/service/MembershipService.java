package com.app.subly.service;

import com.app.subly.dto.subscription.SubscriptionMemberResponse;
import com.app.subly.exception.subscription.MembershipActionForbiddenException;
import com.app.subly.exception.subscription.SubscriptionMemberNotFoundException;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.repository.SubscriptionMembershipRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final SubscriptionAccess subscriptionAccess;
    private final SubscriptionMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public List<SubscriptionMemberResponse> findMembers(String ownerUsername, long subscriptionId) {
        subscriptionAccess.getOwnedSubscription(ownerUsername, subscriptionId);
        return membersOf(subscriptionId);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionMemberResponse> findParticipants(String username, long subscriptionId) {
        subscriptionAccess.getMembership(username, subscriptionId);
        return membersOf(subscriptionId);
    }

    @Transactional
    public void leave(String username, long subscriptionId) {
        SubscriptionMembership membership = subscriptionAccess.getMembership(username, subscriptionId);
        if (membership.getRole() != MembershipRole.MEMBER) {
            throw new MembershipActionForbiddenException("The owner cannot leave their own subscription");
        }
        membershipRepository.delete(membership);
    }

    @Transactional
    public void removeMember(String ownerUsername, long subscriptionId, long membershipId) {
        subscriptionAccess.getOwnedSubscription(ownerUsername, subscriptionId);
        SubscriptionMembership target = membershipRepository
                .findById(membershipId)
                .filter(membership -> membership.getSubscription().getId().equals(subscriptionId))
                .orElseThrow(() -> new SubscriptionMemberNotFoundException(membershipId));
        if (target.getRole() != MembershipRole.MEMBER) {
            throw new MembershipActionForbiddenException("The owner cannot be removed from the subscription");
        }
        membershipRepository.delete(target);
    }

    private List<SubscriptionMemberResponse> membersOf(long subscriptionId) {
        List<SubscriptionMembership> members = membershipRepository.findBySubscriptionIdOrderByIdAsc(subscriptionId);
        return members.stream()
                .map(member -> SubscriptionMemberResponse.from(
                        member, member.getSubscription().shareFor(member.getRole(), members.size())))
                .toList();
    }
}
