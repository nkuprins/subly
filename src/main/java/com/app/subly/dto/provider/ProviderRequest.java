package com.app.subly.dto.provider;

import com.app.subly.model.provider.SubscriptionProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProviderRequest(
        @NotBlank @Size(max = SubscriptionProvider.NAME_MAX_LENGTH)
        String name,

        @NotNull Long categoryId,

        @Size(max = SubscriptionProvider.LOGO_PATH_MAX_LENGTH)
        String logoPath) {}
