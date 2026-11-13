package com.app.subly.dto.subscription;

import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.MembershipRole;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CancelledSubscriptionResponse(
        SubscriptionResponse subscription, MembershipRole role, LocalDate endsOn, BigDecimal saved) {

    public static CancelledSubscriptionResponse from(SubscriptionMembership membership, BigDecimal saved) {
        return new CancelledSubscriptionResponse(
                SubscriptionResponse.from(membership.getSubscription()),
                membership.getRole(),
                membership.getSubscription().getNextPaymentDate(),
                saved);
    }
}
