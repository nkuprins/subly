package com.app.subly.dto.subscription;

import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.SubscriptionMembership;
import java.math.BigDecimal;

public record SubscriptionMemberResponse(Long membershipId, String username, MembershipRole role, BigDecimal share) {
    public static SubscriptionMemberResponse from(SubscriptionMembership membership, BigDecimal share) {
        return new SubscriptionMemberResponse(
                membership.getId(), membership.getUser().getUsername(), membership.getRole(), share);
    }
}
