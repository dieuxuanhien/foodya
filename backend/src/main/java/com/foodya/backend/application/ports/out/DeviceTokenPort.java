package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.DeviceToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenPort {

    DeviceToken save(DeviceToken deviceToken);

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findEnabledByUserId(UUID userId);

    void disableToken(UUID userId, String token);
}
