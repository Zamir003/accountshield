package com.accountshield.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final int MIN_SECRET_BYTES = 32;

    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(JwtProperties properties) {
        this.signingKey = buildSigningKey(properties.secret());
        this.expirationSeconds = validateExpiration(properties.expirationSeconds());
    }

    public String generateToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim(ROLE_CLAIM, principal.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public Optional<String> extractValidEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            // Səbəb (expired, imza səhvdir və s.) yalnız logda; token-in özü loga yazılmır.
            log.debug("Invalid JWT: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    // ---------------- Başlanğıc yoxlamaları ----------------

    private static SecretKey buildSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET environment variable is not set");
        }
        byte[] keyBytes;
        try {
            // Secret Base64 formatında saxlanılır (təsadüfi 32 bayt -> 44 simvol)
            keyBytes = Decoders.BASE64.decode(secret.trim());
        } catch (DecodingException ex) {
            throw new IllegalStateException("JWT_SECRET must be a valid Base64 string");
        }
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short: decoded to " + keyBytes.length + " bytes, at least "
                            + MIN_SECRET_BYTES + " bytes (256 bits) are required for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static long validateExpiration(long seconds) {
        if (seconds <= 0) {
            throw new IllegalStateException("jwt.expiration-seconds must be positive, got: " + seconds);
        }
        return seconds;
    }
}
