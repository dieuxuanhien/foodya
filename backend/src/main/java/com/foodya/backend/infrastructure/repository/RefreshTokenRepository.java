package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.RefreshTokenPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenPersistenceModel, UUID> {
    Optional<RefreshTokenPersistenceModel> findByTokenJti(String tokenJti);

    List<RefreshTokenPersistenceModel> findByTokenFamily(String tokenFamily);

    List<RefreshTokenPersistenceModel> findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, OffsetDateTime now);
}
