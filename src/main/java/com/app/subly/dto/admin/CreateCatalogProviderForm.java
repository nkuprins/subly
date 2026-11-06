package com.app.subly.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCatalogProviderForm {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    private Long categoryId;

    @Size(max = 500)
    private String logoPath;
}
