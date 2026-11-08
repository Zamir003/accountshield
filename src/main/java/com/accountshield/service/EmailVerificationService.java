package com.accountshield.service;

import com.accountshield.entity.User;
import com.accountshield.entity.VerificationToken;
import com.accountshield.enums.UserStatus;
import com.accountshield.enums.VerificationResult;
import com.accountshield.exception.BadRequestException;
import com.accountshield.repository.UserRepository;
import com.accountshield.repository.VerificationTokenRepository;
import com.accountshield.security.TokenGenerator;
import com.accountshield.security.VerificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private static final String INVALID_TOKEN_MESSAGE = "Invalid or expired verification token";

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final Duration tokenTtl;

    public EmailVerificationService(VerificationTokenRepository tokenRepository,
                                    UserRepository userRepository,
                                    VerificationProperties properties) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.tokenTtl = Duration.ofHours(properties.tokenTtlHours());
    }

    @Transactional
    public void sendVerification(User user) {
        tokenRepository.deleteAllByUserId(user.getId());

        String token = TokenGenerator.generate();
        Instant expiresAt = Instant.now().plus(tokenTtl);
        tokenRepository.save(new VerificationToken(user, TokenGenerator.sha256Hex(token), expiresAt));

        log.info("[SIMULATED EMAIL] To: {} | Verification token: {} (expires {}) | "
                        + "POST /api/auth/verify {\"token\":\"{}\"}",
                user.getEmail(), token, expiresAt, token);
    }

    @Transactional
    public VerificationResult verify(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException(INVALID_TOKEN_MESSAGE);
        }

        VerificationToken verificationToken = tokenRepository.findByTokenHash(TokenGenerator.sha256Hex(token.trim()))
                .orElseThrow(() -> new BadRequestException(INVALID_TOKEN_MESSAGE));

        if (verificationToken.isExpiredAt(Instant.now())) {
            throw new BadRequestException(INVALID_TOKEN_MESSAGE);
        }

        User user = verificationToken.getUser();
        VerificationResult result = switch (user.getStatus()) {
            case PENDING_VERIFICATION -> {
                user.setStatus(UserStatus.ACTIVE);
                log.info("Email verified for user id={}", user.getId());
                yield VerificationResult.VERIFIED;
            }
            case ACTIVE -> VerificationResult.ALREADY_VERIFIED;
            case BLOCKED -> throw new BadRequestException(INVALID_TOKEN_MESSAGE);
        };

        tokenRepository.deleteAllByUserId(user.getId());
        return result;
    }

    @Transactional
    public void resendVerification(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmail(normalizedEmail)
                .filter(user -> user.getStatus() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(this::sendVerification);
    }
}
