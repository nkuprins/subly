package com.app.subly.dto.provider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProviderRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull Long categoryId,
        @Size(max = 500) String logoPath) {}
