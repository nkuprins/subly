package com.app.subly.service;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.MembershipRole;
import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
import com.app.subly.repository.SubscriptionMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class SubscriptionAccess {

    private final SubscriptionMembershipRepository membershipRepository;
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionAccess.class);

    SubscriptionMembership getMembership(String username, long subscriptionId) {
        logger.debug("Finding membership for username={} and subscriptionId={}", username, subscriptionId);
        return membershipRepository
                .findBySubscriptionIdAndUserUsername(subscriptionId, username)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
    }

    SubscriptionMembership getOwnerMembership(String username, long subscriptionId) {
        SubscriptionMembership owner = membershipRepository
                .findBySubscriptionIdAndRole(subscriptionId, MembershipRole.OWNER)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        return requireOwnedBy(owner, username, subscriptionId);
    }

    Subscription getOwnedSubscription(String username, long subscriptionId) {
        logger.debug("Getting owned subscription | username={}, subscriptionId={}", username, subscriptionId);
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
