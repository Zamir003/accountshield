package com.accountshield.security;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.lockout")
public record LockoutProperties(
        @Positive int maxAttempts,
        @Positive long durationMinutes
) {
}
