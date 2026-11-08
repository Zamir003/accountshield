package com.accountshield.service;

import com.accountshield.entity.RefreshToken;
import com.accountshield.entity.User;
import com.accountshield.exception.InvalidRefreshTokenException;
import com.accountshield.repository.RefreshTokenRepository;
import com.accountshield.repository.UserRepository;
import com.accountshield.security.RefreshTokenProperties;
import com.accountshield.security.TokenGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository,
                               RefreshTokenProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.ttl = Duration.ofDays(properties.ttlDays());
    }

    public record IssuedRefreshToken(String token, Instant expiresAt) {
    }

    public record RotationResult(User user, IssuedRefreshToken refreshToken) {
    }

    @Transactional
    public IssuedRefreshToken issue(Long userId) {
        User user = userRepository.getReferenceById(userId);
        return createToken(user, UUID.randomUUID(), Instant.now().plus(ttl));
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotationResult rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken current = refreshTokenRepository.findByTokenHash(TokenGenerator.sha256Hex(rawToken.trim()))
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = Instant.now();
        User user = current.getUser();

        if (current.isRevoked()) {
            revokeFamilyAsReuse(current, now, "revoked token reused");
            throw new InvalidRefreshTokenException();
        }

        if (current.isExpiredAt(now)) {
            throw new InvalidRefreshTokenException();
        }

        if (!user.isActive()) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }

        if (refreshTokenRepository.revokeIfActive(current.getId(), now) == 0) {
            revokeFamilyAsReuse(current, now, "concurrent use of the same token");
            throw new InvalidRefreshTokenException();
        }

        IssuedRefreshToken next = createToken(user, current.getFamilyId(), current.getExpiresAt());
        return new RotationResult(user, next);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(TokenGenerator.sha256Hex(rawToken.trim()))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now()));
    }

    private IssuedRefreshToken createToken(User user, UUID familyId, Instant expiresAt) {
        String rawToken = TokenGenerator.generate();
        refreshTokenRepository.save(new RefreshToken(user, TokenGenerator.sha256Hex(rawToken), familyId, expiresAt));
        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    private void revokeFamilyAsReuse(RefreshToken token, Instant now, String reason) {
        int revoked = refreshTokenRepository.revokeFamily(token.getFamilyId(), now);
        log.warn("Refresh token reuse detected ({}): userId={}, familyId={}, revoked {} active token(s)",
                reason, token.getUser().getId(), token.getFamilyId(), revoked);
    }
}
