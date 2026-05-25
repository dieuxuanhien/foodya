package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.DeviceTokenPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceTokenPersistenceModel, UUID> {

    Optional<DeviceTokenPersistenceModel> findByToken(String token);

    List<DeviceTokenPersistenceModel> findByUserIdAndEnabledTrue(UUID userId);

    Optional<DeviceTokenPersistenceModel> findByUserIdAndToken(UUID userId, String token);
}
