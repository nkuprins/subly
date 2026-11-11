package com.app.subly.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class CreateCategoryForm {
    @NotBlank(message = "You must provide a valid, non-empty title")
    @Size(max = 100, message = "Title length must be lower than 100 chars.")
    private String title;
}
