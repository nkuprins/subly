package com.app.subly.service.access;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.MembershipRole;
import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
import com.app.subly.repository.SubscriptionMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class SubscriptionAccess {

    private final SubscriptionMembershipRepository membershipRepository;

    public SubscriptionMembership getMembership(String username, long subscriptionId) {
        return membershipRepository
                .findBySubscriptionIdAndUserUsername(subscriptionId, username)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
    }

    public SubscriptionMembership getOwnerMembership(String username, long subscriptionId) {
        SubscriptionMembership owner = membershipRepository
                .findBySubscriptionIdAndRole(subscriptionId, MembershipRole.OWNER)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        return requireOwnedBy(owner, username, subscriptionId);
    }

    public Subscription getOwnedSubscription(String username, long subscriptionId) {
        return getOwnerMembership(username, subscriptionId).getSubscription();
    }

    private static SubscriptionMembership requireOwnedBy(
            SubscriptionMembership owner, String username, long subscriptionId) {
        if (!owner.getUser().getUsername().equals(username)) {
            throw new SubscriptionAccessDeniedException(subscriptionId);
        }
        return owner;
    }
}
