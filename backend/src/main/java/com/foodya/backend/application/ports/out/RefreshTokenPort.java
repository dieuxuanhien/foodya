package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.RefreshToken;
import com.foodya.backend.domain.entities.UserAccount;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenPort {

    Optional<RefreshToken> findByTokenJti(String tokenJti);

    List<RefreshToken> findByTokenFamily(String tokenFamily);

    List<RefreshToken> findByUserAndRevokedAtIsNullAndExpiresAtAfter(UserAccount user, OffsetDateTime now);

    RefreshToken save(RefreshToken refreshToken);

    List<RefreshToken> saveAll(List<RefreshToken> refreshTokens);
}
