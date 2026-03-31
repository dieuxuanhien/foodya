package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.ports.out.DeliveryTrackingPointPort;
import com.foodya.backend.domain.entities.DeliveryTrackingPoint;
import com.foodya.backend.infrastructure.repository.DeliveryTrackingPointRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class DeliveryTrackingPointPersistenceAdapter implements DeliveryTrackingPointPort {

    private final DeliveryTrackingPointRepository repository;

    public DeliveryTrackingPointPersistenceAdapter(DeliveryTrackingPointRepository repository) {
        this.repository = repository;
    }

    @Override
    public DeliveryTrackingPoint save(DeliveryTrackingPoint point) {
        return repository.save(Objects.requireNonNull(point));
    }

    @Override
    public List<DeliveryTrackingPoint> findByOrderIdOrderByRecordedAtAsc(UUID orderId) {
        return repository.findByOrderIdOrderByRecordedAtAsc(Objects.requireNonNull(orderId));
    }

    @Override
    public long deleteByRecordedAtBefore(OffsetDateTime cutoff) {
        return repository.deleteByRecordedAtBefore(Objects.requireNonNull(cutoff));
    }
}
