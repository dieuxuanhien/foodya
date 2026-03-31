package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.persistence.DeliveryTrackingPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryTrackingPointRepository extends JpaRepository<DeliveryTrackingPoint, UUID> {

    List<DeliveryTrackingPoint> findByOrderIdOrderByRecordedAtAsc(UUID orderId);
}
