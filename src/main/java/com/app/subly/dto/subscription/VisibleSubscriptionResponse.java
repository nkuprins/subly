package com.app.subly.dto.subscription;

import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.SubscriptionMembership;

public record VisibleSubscriptionResponse(SubscriptionResponse subscription, MembershipRole role) {
    public static VisibleSubscriptionResponse from(SubscriptionMembership membership) {
        return new VisibleSubscriptionResponse(
                SubscriptionResponse.from(membership.getSubscription()), membership.getRole());
    }
}
