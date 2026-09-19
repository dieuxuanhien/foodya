package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.DriverOnlineSessionPort;
import com.foodya.backend.domain.entities.DriverOnlineSession;
import com.foodya.backend.infrastructure.mapper.DriverOnlineSessionMapper;
import com.foodya.backend.infrastructure.repository.DriverOnlineSessionRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Persistence adapter implementing {@link DriverOnlineSessionPort}.
 * Bridges the domain layer to the Spring Data JPA repository.
 */
@Component
public class DriverOnlineSessionAdapter implements DriverOnlineSessionPort {

    private final DriverOnlineSessionRepository repository;
    private final DriverOnlineSessionMapper mapper;

    public DriverOnlineSessionAdapter(DriverOnlineSessionRepository repository,
                                      DriverOnlineSessionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<DriverOnlineSession> findActiveSession(UUID driverUserId) {
        return repository.findActiveByDriverUserId(Objects.requireNonNull(driverUserId))
                .map(mapper::toDomain);
    }

    @Override
    @SuppressWarnings("null")
    public DriverOnlineSession save(DriverOnlineSession session) {
        var model = mapper.toPersistence(Objects.requireNonNull(session));
        var saved = repository.save(model);
        return mapper.toDomain(saved);
    }

    @Override
    public List<DriverOnlineSession> findOnlineDriversInH3Cells(Set<String> h3Indexes) {
        if (h3Indexes == null || h3Indexes.isEmpty()) {
            return List.of();
        }
        return repository.findOnlineDriversInH3Cells(h3Indexes)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void updateHeartbeat(UUID sessionId, Instant now) {
        repository.updateHeartbeat(
                Objects.requireNonNull(sessionId),
                OffsetDateTime.ofInstant(Objects.requireNonNull(now), ZoneOffset.UTC));
    }
}
