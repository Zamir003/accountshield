package com.accountshield.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;


public final class TokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private TokenGenerator() {
    }

    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    public static String sha256Hex(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 hər Java tətbiqində mövcud olmalıdır (Java spesifikasiyası tələb edir)
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
