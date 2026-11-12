package com.app.subly.service.share;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class MemberShareLoader {

    private final SubscriptionMembershipRepository membershipRepository;

    public List<MemberShare> forUser(String username, SubscriptionStatus status) {
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
                memberships.stream().map(MemberShareLoader::subscriptionId).toList();
        return membershipRepository.findBySubscriptionIdIn(subscriptionIds).stream()
                .collect(Collectors.groupingBy(MemberShareLoader::subscriptionId, Collectors.counting()));
    }

    private static MemberShare shareOf(SubscriptionMembership membership, Map<Long, Long> memberCounts) {
        Subscription subscription = membership.getSubscription();
        long memberCount = memberCounts.get(subscription.getId());
        return MemberShare.of(membership, memberCount);
    }

    private static Long subscriptionId(SubscriptionMembership membership) {
        return membership.getSubscription().getId();
    }
}
