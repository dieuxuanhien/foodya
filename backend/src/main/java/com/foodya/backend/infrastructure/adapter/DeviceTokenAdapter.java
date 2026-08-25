package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.DeviceTokenPort;
import com.foodya.backend.domain.entities.DeviceToken;
import com.foodya.backend.infrastructure.persistence.models.DeviceTokenPersistenceModel;
import com.foodya.backend.infrastructure.repository.DeviceTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DeviceTokenAdapter implements DeviceTokenPort {

    private final DeviceTokenRepository repository;

    public DeviceTokenAdapter(DeviceTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public DeviceToken save(DeviceToken deviceToken) {
        DeviceTokenPersistenceModel entity = repository.findByToken(deviceToken.getToken())
                .orElseGet(DeviceTokenPersistenceModel::new);
        copyToEntity(deviceToken, entity);
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<DeviceToken> findByToken(String token) {
        return repository.findByToken(token).map(this::toDomain);
    }

    @Override
    public List<DeviceToken> findEnabledByUserId(UUID userId) {
        return repository.findByUserIdAndEnabledTrue(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void disableToken(UUID userId, String token) {
        repository.findByUserIdAndToken(userId, token).ifPresent(entity -> {
            entity.setEnabled(false);
            entity.setUpdatedAt(OffsetDateTime.now());
            repository.save(entity);
        });
    }

    private void copyToEntity(DeviceToken source, DeviceTokenPersistenceModel target) {
        if (target.getId() == null) {
            target.setId(source.getId());
        }
        target.setUserId(source.getUserId());
        target.setToken(source.getToken());
        target.setPlatform(source.getPlatform());
        target.setDeviceId(source.getDeviceId());
        target.setAppVersion(source.getAppVersion());
        target.setEnabled(source.isEnabled());
        target.setLastSeenAt(source.getLastSeenAt());
        if (target.getCreatedAt() == null) {
            target.setCreatedAt(source.getCreatedAt());
        }
        target.setUpdatedAt(source.getUpdatedAt());
    }

    private DeviceToken toDomain(DeviceTokenPersistenceModel entity) {
        DeviceToken model = new DeviceToken();
        model.setId(entity.getId());
        model.setUserId(entity.getUserId());
        model.setToken(entity.getToken());
        model.setPlatform(entity.getPlatform());
        model.setDeviceId(entity.getDeviceId());
        model.setAppVersion(entity.getAppVersion());
        model.setEnabled(entity.isEnabled());
        model.setLastSeenAt(entity.getLastSeenAt());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());
        return model;
    }
}
