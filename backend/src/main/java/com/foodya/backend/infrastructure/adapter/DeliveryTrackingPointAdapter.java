package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.DeliveryTrackingPointPort;
import com.foodya.backend.domain.entities.DeliveryTrackingPoint;
import com.foodya.backend.infrastructure.mapper.DeliveryTrackingPointMapper;
import com.foodya.backend.infrastructure.repository.DeliveryTrackingPointRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class DeliveryTrackingPointAdapter implements DeliveryTrackingPointPort {

    private final DeliveryTrackingPointRepository repository;
    private final DeliveryTrackingPointMapper mapper;

    public DeliveryTrackingPointAdapter(DeliveryTrackingPointRepository repository,
                                                   DeliveryTrackingPointMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @SuppressWarnings("null")
    public DeliveryTrackingPoint save(DeliveryTrackingPoint point) {
        var saved = repository.save(mapper.toPersistence(Objects.requireNonNull(point)));
        return mapper.toDomain(saved);
    }

    @Override
    public List<DeliveryTrackingPoint> findByOrderIdOrderByRecordedAtAsc(UUID orderId) {
        return repository.findByOrderIdOrderByRecordedAtAsc(Objects.requireNonNull(orderId))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long deleteByRecordedAtBefore(OffsetDateTime cutoff) {
        return repository.deleteByRecordedAtBefore(Objects.requireNonNull(cutoff));
    }
}
