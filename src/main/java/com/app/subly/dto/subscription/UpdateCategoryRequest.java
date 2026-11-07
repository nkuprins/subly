package com.app.subly.dto.subscription;

import com.app.subly.model.subscription.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Positive @NotNull Long id,
        @NotBlank @Size(max = Category.NAME_MAX_LENGTH) String newName) {}
