package com.app.subly.service;

import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.repository.SubscriptionMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SubscriptionAccess {

    private final SubscriptionMembershipRepository membershipRepository;

    Subscription getOwnedSubscription(String username, long subscriptionId) {
        return getOwnerMembership(username, subscriptionId).getSubscription();
    }

    SubscriptionMembership getOwnerMembership(String username, long subscriptionId) {
        SubscriptionMembership owner = membershipRepository
                .findBySubscriptionIdAndRole(subscriptionId, MembershipRole.OWNER)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        return requireOwnedBy(owner, username, subscriptionId);
    }

    SubscriptionMembership getOwnerMembershipWithSubscriptionDetails(String username, long subscriptionId) {
        SubscriptionMembership owner = membershipRepository
                .findWithSubscriptionDetailsBySubscriptionIdAndRole(subscriptionId, MembershipRole.OWNER)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        return requireOwnedBy(owner, username, subscriptionId);
    }

    private static SubscriptionMembership requireOwnedBy(
            SubscriptionMembership owner, String username, long subscriptionId) {
        if (!owner.getUser().getUsername().equals(username)) {
            throw new SubscriptionAccessDeniedException(subscriptionId);
        }
        return owner;
    }
}
