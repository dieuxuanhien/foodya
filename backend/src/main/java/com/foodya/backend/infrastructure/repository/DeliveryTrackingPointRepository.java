package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.DeliveryTrackingPointPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DeliveryTrackingPointRepository extends JpaRepository<DeliveryTrackingPointPersistenceModel, UUID> {

    List<DeliveryTrackingPointPersistenceModel> findByOrderIdOrderByRecordedAtAsc(UUID orderId);

    long deleteByRecordedAtBefore(OffsetDateTime cutoff);
}
