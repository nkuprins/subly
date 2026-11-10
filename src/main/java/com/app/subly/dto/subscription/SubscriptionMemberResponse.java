package com.app.subly.dto.subscription;

import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.SubscriptionMembership;

public record SubscriptionMemberResponse(Long membershipId, String username, MembershipRole role) {
    public static SubscriptionMemberResponse from(SubscriptionMembership membership) {
        return new SubscriptionMemberResponse(
                membership.getId(), membership.getUser().getUsername(), membership.getRole());
    }
}
