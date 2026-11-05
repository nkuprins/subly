package com.app.subly.dto.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateCustomCategoryRequest {

    @NotBlank
    @Size(max=100)
    private String categoryName;
}
