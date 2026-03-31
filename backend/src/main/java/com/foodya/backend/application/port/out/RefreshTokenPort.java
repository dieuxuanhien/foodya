package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.RefreshToken;
import com.foodya.backend.domain.persistence.UserAccount;

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
