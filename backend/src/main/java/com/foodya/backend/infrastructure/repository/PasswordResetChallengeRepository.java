package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.entities.PasswordResetChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetChallengeRepository extends JpaRepository<PasswordResetChallenge, UUID> {
    Optional<PasswordResetChallenge> findByChallengeToken(String challengeToken);
}
