package com.app.subly.dto.invitation;

import com.app.subly.entity.invitation.PendingInvitation;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import java.math.BigDecimal;

public record ReceivedInvitationResponse(
        Long id,
        String providerName,
        String ownerUsername,
        BigDecimal price,
        Currency currency,
        BillingCycle billingCycle) {

    public static ReceivedInvitationResponse from(PendingInvitation invitation) {
        Subscription subscription = invitation.getSubscription();
        return new ReceivedInvitationResponse(
                invitation.getId(),
                subscription.getProvider().getName(),
                invitation.getInvitedBy().getUsername(),
                subscription.getPrice(),
                subscription.getCurrency(),
                subscription.getBillingCycle());
    }
}
