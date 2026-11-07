package com.app.subly.dto.subscription;

import com.app.subly.model.subscription.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateCustomCategoryRequest {

    @NotBlank
    @Size(max = Category.NAME_MAX_LENGTH)
    private String categoryName;
}
