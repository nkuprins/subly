package com.app.subly.services;

import com.app.subly.exceptions.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exceptions.subscription.SubscriptionNotFoundException;
import com.app.subly.models.subscription.MembershipRole;
import com.app.subly.models.subscription.Subscription;
import com.app.subly.models.subscription.SubscriptionMembership;
import com.app.subly.repository.SubscriptionMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SubscriptionAccess {

    private final SubscriptionMembershipRepository membershipRepository;

    // One query for the OWNER row replaces the old two (load subscription, then check owner).
    Subscription getOwnedSubscription(String username, long subscriptionId) {
        SubscriptionMembership owner = membershipRepository
                .findBySubscriptionIdAndRole(subscriptionId, MembershipRole.OWNER)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        if (!owner.getUser().getUsername().equals(username)) {
            throw new SubscriptionAccessDeniedException(subscriptionId);
        }
        return owner.getSubscription();
    }
}
