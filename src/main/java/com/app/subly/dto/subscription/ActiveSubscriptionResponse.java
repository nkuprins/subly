package com.app.subly.dto.subscription;

import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.SubscriptionMembership;
import java.math.BigDecimal;

public record ActiveSubscriptionResponse(SubscriptionResponse subscription, MembershipRole role, BigDecimal share) {
    public static ActiveSubscriptionResponse from(SubscriptionMembership membership, BigDecimal share) {
        return new ActiveSubscriptionResponse(
                SubscriptionResponse.from(membership.getSubscription()), membership.getRole(), share);
    }
}
