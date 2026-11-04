package com.app.subly.dto.subscription;

import com.app.subly.models.subscription.BillingCycle;
import com.app.subly.models.subscription.Category;
import com.app.subly.models.subscription.Currency;
import com.app.subly.models.subscription.SubscriptionProvider;
import com.app.subly.models.subscription.ProviderType;
import com.app.subly.models.subscription.Subscription;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(
        Long id,
        String providerName,
        Long categoryId,
        String categoryName,
        ProviderType providerType,
        BigDecimal price,
        Currency currency,
        BillingCycle billingCycle,
        LocalDate nextPaymentDate,
        boolean freeTrial) {

    public static SubscriptionResponse from(Subscription subscription) {
        SubscriptionProvider provider = subscription.getProvider();
        Category category = provider.getCategory();
        return new SubscriptionResponse(
                subscription.getId(),
                provider.getName(),
                category.getId(),
                category.getName(),
                provider.getProviderType(),
                subscription.getPrice(),
                subscription.getCurrency(),
                subscription.getBillingCycle(),
                subscription.getNextPaymentDate(),
                subscription.isFreeTrial());
    }
}
