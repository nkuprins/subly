package com.app.subly.config;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "emails.reminder-service")
@RequiredArgsConstructor
@Getter
public class ReminderServiceConfig {
    @Email
    @NotBlank
    private final String defaultEmail;

    @Min(value = 500, message = "The post delay cannot be less than 500 milliseconds")
    @Max(value = 10000, message = "The post delay cannot be more than 10000 ms")
    private final Long postDelayMs;

    @Min(value = 1000, message = "Retry delay cannot be lower than 1000 ms")
    @Max(value = 10000, message = "Retry delay max value is 10000 ms")
    private final Long retryDelayMs;

    @Positive
    @Max(value = 10, message = "Retry attempts cannot be more than 10")
    private final int retryAttempts;
}
