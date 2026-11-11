package com.app.subly.config;

import com.app.subly.dto.user.PasswordRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.admin-settings")
@Getter
@Validated
@RequiredArgsConstructor
public class AdminConfiguration {

    @NotBlank
    private final String adminUsername;

    @NotBlank
    private final String adminName;

    @NotBlank
    @Email
    private final String adminEmail;

    @NotBlank
    @Size(min = PasswordRules.MIN_LENGTH, max = PasswordRules.MAX_LENGTH)
    private final String adminPassword;
}
