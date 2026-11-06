package com.app.subly.dto.invitation;

import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.PendingInvitation;
import com.app.subly.model.subscription.Subscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceivedInvitationResponse(
        Long id,
        Long subscriptionId,
        String serviceName,
        String ownerUsername,
        BigDecimal price,
        Currency currency,
        BillingCycle billingCycle,
        LocalDateTime invitedAt) {

    public static ReceivedInvitationResponse from(PendingInvitation invitation, String ownerUsername) {
        Subscription subscription = invitation.getSubscription();
        return new ReceivedInvitationResponse(
                invitation.getId(),
                subscription.getId(),
                subscription.getService().getName(),
                ownerUsername,
                subscription.getPrice(),
                subscription.getCurrency(),
                subscription.getBillingCycle(),
                invitation.getInvitedAt());
    }
}
