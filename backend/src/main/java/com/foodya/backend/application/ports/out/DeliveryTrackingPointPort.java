package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.DeliveryTrackingPoint;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DeliveryTrackingPointPort {

    DeliveryTrackingPoint save(DeliveryTrackingPoint point);

    List<DeliveryTrackingPoint> findByOrderIdOrderByRecordedAtAsc(UUID orderId);

    long deleteByRecordedAtBefore(OffsetDateTime cutoff);
}
