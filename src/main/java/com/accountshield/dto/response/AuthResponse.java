package com.accountshield.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String refreshToken,
        long refreshExpiresInSeconds
) {

    public static AuthResponse bearer(String accessToken, long expiresInSeconds,
                                      String refreshToken, long refreshExpiresInSeconds) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, refreshToken, refreshExpiresInSeconds);
    }
}
