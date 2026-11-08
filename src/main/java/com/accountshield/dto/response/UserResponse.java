package com.accountshield.dto.response;

import com.accountshield.enums.Role;
import com.accountshield.enums.UserStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        UserStatus status,
        Instant lockedUntil,
        Instant createdAt,
        Instant updatedAt
) {
}
