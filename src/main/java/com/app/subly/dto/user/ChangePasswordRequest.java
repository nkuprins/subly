package com.app.subly.dto.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(min = PasswordRules.MIN_LENGTH, max = PasswordRules.MAX_LENGTH)
        String oldPassword,

        @NotBlank @Size(min = PasswordRules.MIN_LENGTH, max = PasswordRules.MAX_LENGTH)
        String newPassword) {

    @AssertTrue(message = "New password and old password must be different")
    public boolean isNewPasswordDifferent() {
        return oldPassword == null || !oldPassword.equals(newPassword);
    }
}
