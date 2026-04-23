package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.RefreshTokenData;
import com.foodya.backend.application.dto.UserAccountData;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenPort {

    Optional<RefreshTokenData> findByTokenJti(String tokenJti);

    List<RefreshTokenData> findByTokenFamily(String tokenFamily);

    List<RefreshTokenData> findByUserAndRevokedAtIsNullAndExpiresAtAfter(UserAccountData user, OffsetDateTime now);

    RefreshTokenData save(RefreshTokenData refreshToken);

    List<RefreshTokenData> saveAll(List<RefreshTokenData> refreshTokens);
}
