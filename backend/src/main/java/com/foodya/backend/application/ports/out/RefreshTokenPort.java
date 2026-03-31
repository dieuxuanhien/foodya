package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.RefreshTokenModel;
import com.foodya.backend.application.dto.UserAccountModel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenPort {

    Optional<RefreshTokenModel> findByTokenJti(String tokenJti);

    List<RefreshTokenModel> findByTokenFamily(String tokenFamily);

    List<RefreshTokenModel> findByUserAndRevokedAtIsNullAndExpiresAtAfter(UserAccountModel user, OffsetDateTime now);

    RefreshTokenModel save(RefreshTokenModel refreshToken);

    List<RefreshTokenModel> saveAll(List<RefreshTokenModel> refreshTokens);
}
