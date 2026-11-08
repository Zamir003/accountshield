package com.accountshield.mapper;

import com.accountshield.dto.request.RegisterRequest;
import com.accountshield.dto.response.UserResponse;
import com.accountshield.entity.User;
import org.springframework.stereotype.Component;

import java.time.Instant;


@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                // Vaxtı keçmiş kilid bazada qalır (avtomatik silinmir), amma artıq kilid deyil
                user.isLockedAt(Instant.now()) ? user.getLockedUntil() : null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public User toEntity(RegisterRequest request, String normalizedEmail, String passwordHash) {
        return new User(request.name().trim(), normalizedEmail, passwordHash);
    }
}
