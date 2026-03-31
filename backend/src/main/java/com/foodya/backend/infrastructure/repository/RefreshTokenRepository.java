package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenJti(String tokenJti);

    List<RefreshToken> findByTokenFamily(String tokenFamily);

    List<RefreshToken> findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, OffsetDateTime now);
}
