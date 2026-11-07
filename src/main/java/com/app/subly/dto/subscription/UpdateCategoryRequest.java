package com.app.subly.dto.subscription;

import com.app.subly.model.subscription.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UpdateCategoryRequest {

    @Positive
    @NotNull
    private Long id;

    @NotBlank
    @Size(max = Category.NAME_MAX_LENGTH)
    private String newName;
}
