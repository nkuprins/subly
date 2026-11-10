package com.app.subly.dto.subscription;

import com.app.subly.model.category.Category;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SubscriptionResponse(
        Long id,
        Long providerId,
        String providerName,
        Long categoryId,
        String categoryName,
        ProviderType providerType,
        BigDecimal price,
        Currency currency,
        BillingCycle billingCycle,
        LocalDate nextPaymentDate,
        boolean freeTrial,
        int maxParticipants,
        SubscriptionStatus status,
        Instant cancelledAt) {

    public static SubscriptionResponse from(Subscription subscription) {
        SubscriptionProvider provider = subscription.getProvider();
        Category category = provider.getCategory();
        return new SubscriptionResponse(
                subscription.getId(),
                provider.getId(),
                provider.getName(),
                category.getId(),
                category.getName(),
                provider.getProviderType(),
                subscription.getPrice(),
                subscription.getCurrency(),
                subscription.getBillingCycle(),
                subscription.getNextPaymentDate(),
                subscription.isFreeTrial(),
                subscription.getMaxParticipants(),
                subscription.getStatus(),
                subscription.getCancelledAt());
    }
}
