package com.app.subly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChangePasswordRequest {
    @NotBlank
    @Size(min = 15, max = 64)
    private String oldPassword;

    @NotBlank
    @Size(min = 15, max = 64)
    private String newPassword;
}
