package com.app.subly.dto.provider;

import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.enums.ProviderType;

public record ProviderResponse(Long id, String name, String logoPath, ProviderType providerType) {

    public static ProviderResponse from(SubscriptionProvider provider) {
        return new ProviderResponse(
                provider.getId(), provider.getName(), provider.getLogoPath(), provider.getProviderType());
    }
}
