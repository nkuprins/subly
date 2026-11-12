package com.app.subly.service;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.util.MemberShare;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class SubscriptionShares {

    private final SubscriptionMembershipRepository membershipRepository;

    List<MemberShare> forUser(String username, SubscriptionStatus status) {
        List<SubscriptionMembership> memberships = membershipRepository.findMembershipsOf(username, status);
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> memberCounts = countMembers(memberships);
        return memberships.stream()
                .map(membership -> shareOf(membership, memberCounts))
                .toList();
    }

    private Map<Long, Long> countMembers(List<SubscriptionMembership> memberships) {
        List<Long> subscriptionIds =
                memberships.stream().map(SubscriptionShares::subscriptionId).toList();
        return membershipRepository.findBySubscriptionIdIn(subscriptionIds).stream()
                .collect(Collectors.groupingBy(SubscriptionShares::subscriptionId, Collectors.counting()));
    }

    private static MemberShare shareOf(SubscriptionMembership membership, Map<Long, Long> memberCounts) {
        Subscription subscription = membership.getSubscription();
        long memberCount = memberCounts.get(subscription.getId());
        return new MemberShare(membership, subscription.shareFor(membership.getRole(), memberCount));
    }

    private static Long subscriptionId(SubscriptionMembership membership) {
        return membership.getSubscription().getId();
    }
}
