package com.accountshield.service;

import com.accountshield.repository.UserRepository;
import com.accountshield.security.LockoutProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final UserRepository userRepository;
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttemptService(UserRepository userRepository, LockoutProperties properties) {
        this.userRepository = userRepository;
        this.maxAttempts = properties.maxAttempts();
        this.lockDuration = Duration.ofMinutes(properties.durationMinutes());
    }

    @Transactional
    public void recordFailure(String email) {
        String normalizedEmail = normalizeEmail(email);
        Instant now = Instant.now();

        int updated = userRepository.incrementFailedAttempts(normalizedEmail, now);
        if (updated == 0) {
            return;
        }

        int locked = userRepository.lockIfLimitReached(normalizedEmail, maxAttempts, now.plus(lockDuration));
        if (locked > 0) {
            log.warn("Account locked for {} minutes after {} failed login attempts: {}",
                    lockDuration.toMinutes(), maxAttempts, normalizedEmail);
        }
    }

    @Transactional
    public void recordSuccess(Long userId) {
        userRepository.resetLoginAttempts(userId);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
