package com.accountshield.repository;

import com.accountshield.entity.VerificationToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<VerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
