package com.app.subly.service;

import com.app.subly.dto.subscription.MemberListResponse;
import com.app.subly.dto.subscription.SubscriptionMemberResponse;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.MembershipRole;
import com.app.subly.exception.subscription.MembershipActionForbiddenException;
import com.app.subly.exception.subscription.SubscriptionMemberNotFoundException;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.service.access.SubscriptionAccess;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipService {

    private final SubscriptionAccess subscriptionAccess;
    private final SubscriptionMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public MemberListResponse findMembers(String username, long subscriptionId) {
        log.debug("Getting all members for username={} subscriptionId={}", username, subscriptionId);
        Subscription subscription =
                subscriptionAccess.getMembership(username, subscriptionId).getSubscription();
        List<SubscriptionMemberResponse> members = membersOf(subscriptionId);
        boolean full = subscription.isClosedToNewMembers(members.size());
        return new MemberListResponse(members, full);
    }

    @Transactional
    public void leave(String username, long subscriptionId) {
        SubscriptionMembership membership = subscriptionAccess.getMembership(username, subscriptionId);
        requireMemberRole(membership, "The owner cannot leave their own subscription");
        membershipRepository.delete(membership);
    }

    @Transactional
    public void removeMember(String ownerUsername, long subscriptionId, long membershipId) {
        subscriptionAccess.getOwnedSubscription(ownerUsername, subscriptionId);
        SubscriptionMembership target = membershipRepository
                .findByIdAndSubscriptionId(membershipId, subscriptionId)
                .orElseThrow(() -> new SubscriptionMemberNotFoundException(membershipId));
        requireMemberRole(target, "The owner cannot be removed from the subscription");
        membershipRepository.delete(target);
    }

    private static void requireMemberRole(SubscriptionMembership membership, String message) {
        if (membership.getRole() != MembershipRole.MEMBER) {
            throw new MembershipActionForbiddenException(message);
        }
    }

    private List<SubscriptionMemberResponse> membersOf(long subscriptionId) {
        List<SubscriptionMembership> members = membershipRepository.findBySubscriptionIdOrderByIdAsc(subscriptionId);
        return members.stream()
                .map(member -> SubscriptionMemberResponse.from(
                        member, member.getSubscription().shareFor(member.getRole(), members.size())))
                .toList();
    }
}
