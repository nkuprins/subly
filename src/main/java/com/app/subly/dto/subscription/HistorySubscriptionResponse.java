package com.app.subly.dto.subscription;

import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.SubscriptionMembership;
import java.math.BigDecimal;
import java.time.LocalDate;

public record HistorySubscriptionResponse(
        SubscriptionResponse subscription, MembershipRole role, LocalDate endedOn, BigDecimal saved) {

    public static HistorySubscriptionResponse from(SubscriptionMembership membership, BigDecimal saved) {
        return new HistorySubscriptionResponse(
                SubscriptionResponse.from(membership.getSubscription()),
                membership.getRole(),
                membership.getSubscription().getNextPaymentDate(),
                saved);
    }
}
