package com.app.subly.dto.provider;

import com.app.subly.model.subscription.ProviderType;
import com.app.subly.model.subscription.SubscriptionProvider;

public record ProviderResponse(Long id, String name, String categoryName, String logoPath, ProviderType providerType) {

    public static ProviderResponse from(SubscriptionProvider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getName(),
                provider.getCategory().getName(),
                provider.getLogoPath(),
                provider.getProviderType());
    }
}
