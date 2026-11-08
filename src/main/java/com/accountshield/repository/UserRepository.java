package com.accountshield.repository;

import com.accountshield.entity.User;
import com.accountshield.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 "
            + "WHERE u.email = :email AND (u.lockedUntil IS NULL OR u.lockedUntil <= :now)")
    int incrementFailedAttempts(@Param("email") String email, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = :lockedUntil, u.failedLoginAttempts = 0 "
            + "WHERE u.email = :email AND u.failedLoginAttempts >= :maxAttempts")
    int lockIfLimitReached(@Param("email") String email,
                           @Param("maxAttempts") int maxAttempts,
                           @Param("lockedUntil") Instant lockedUntil);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = NULL "
            + "WHERE u.id = :id AND (u.failedLoginAttempts > 0 OR u.lockedUntil IS NOT NULL)")
    int resetLoginAttempts(@Param("id") Long id);
}
