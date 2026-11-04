package com.app.subly.dto.subscription;

import com.app.subly.models.subscription.BillingCycle;
import com.app.subly.models.subscription.Category;
import com.app.subly.models.subscription.Currency;
import com.app.subly.models.subscription.ServiceProvider;
import com.app.subly.models.subscription.ServiceType;
import com.app.subly.models.subscription.Subscription;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(
        Long id,
        String serviceName,
        Long categoryId,
        String categoryName,
        ServiceType serviceType,
        BigDecimal price,
        Currency currency,
        BillingCycle billingCycle,
        LocalDate nextPaymentDate,
        boolean freeTrial) {

    public static SubscriptionResponse from(Subscription subscription) {
        ServiceProvider service = subscription.getService();
        Category category = service.getCategory();
        return new SubscriptionResponse(
                subscription.getId(),
                service.getName(),
                category.getId(),
                category.getName(),
                service.getServiceType(),
                subscription.getPrice(),
                subscription.getCurrency(),
                subscription.getBillingCycle(),
                subscription.getNextPaymentDate(),
                subscription.isFreeTrial());
    }
}
