package com.app.subly.dto.category;

import com.app.subly.model.category.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomCategoryRequest(
        @NotBlank @Size(max = Category.NAME_MAX_LENGTH) String categoryName) {}
