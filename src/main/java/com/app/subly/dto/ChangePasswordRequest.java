package com.app.subly.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChangePasswordRequest {
    @NotBlank
    @Size(min = PasswordRules.MIN_LENGTH, max = PasswordRules.MAX_LENGTH)
    private String oldPassword;

    @NotBlank
    @Size(min = PasswordRules.MIN_LENGTH, max = PasswordRules.MAX_LENGTH)
    private String newPassword;

    @AssertTrue(message = "New password and old password must be different")
    public boolean isNewPasswordDifferent() {
        return oldPassword == null || !oldPassword.equals(newPassword);
    }
}
