package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.PasswordResetChallengePersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetChallengeRepository extends JpaRepository<PasswordResetChallengePersistenceModel, UUID> {
    Optional<PasswordResetChallengePersistenceModel> findByChallengeToken(String challengeToken);
}
