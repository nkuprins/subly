package com.app.subly.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "emails.resend-api")
@RequiredArgsConstructor
@Getter
public class ResendAPIClientConfiguration {
    @NotBlank
    private final String apiKey;

    @NotBlank
    @URL
    private final String emailUrl;
}
