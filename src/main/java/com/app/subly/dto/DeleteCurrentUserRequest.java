package com.app.subly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteCurrentUserRequest {
    @NotBlank
    @Size(min = PasswordRules.MIN_LENGTH, max = PasswordRules.MAX_LENGTH)
    private String password;
}
