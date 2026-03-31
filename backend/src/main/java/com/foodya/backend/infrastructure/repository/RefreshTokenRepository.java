package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.persistence.RefreshToken;
import com.foodya.backend.domain.persistence.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenJti(String tokenJti);

    List<RefreshToken> findByTokenFamily(String tokenFamily);

    List<RefreshToken> findByUserAndRevokedAtIsNullAndExpiresAtAfter(UserAccount user, OffsetDateTime now);
}
