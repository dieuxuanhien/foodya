package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.DeliveryTrackingPoint;

import java.util.List;
import java.util.UUID;

public interface DeliveryTrackingPointPort {

    DeliveryTrackingPoint save(DeliveryTrackingPoint point);

    List<DeliveryTrackingPoint> findByOrderIdOrderByRecordedAtAsc(UUID orderId);
}
