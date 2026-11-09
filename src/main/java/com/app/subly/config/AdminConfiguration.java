package com.app.subly.config;

import com.app.subly.dto.user.PasswordRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.admin-settings")
@Component
@Setter
@Getter
@Validated
public class AdminConfiguration {

    @NotBlank
    private String adminUsername;

    @NotBlank
    private String adminName;

    @NotBlank
    @Email
    private String adminEmail;

    @NotBlank
    @Size(min = PasswordRules.MIN_LENGTH, max = PasswordRules.MAX_LENGTH)
    private String adminPassword;
}
