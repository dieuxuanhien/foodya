package com.foodya.backend.infrastructure.adapter.event;

import com.foodya.backend.application.dto.OrderTrackingPointView;
import com.foodya.backend.application.ports.out.OrderTrackingUpdatePublisherPort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class StompOrderTrackingUpdatePublisherAdapter implements OrderTrackingUpdatePublisherPort {

    private final SimpMessagingTemplate messagingTemplate;

    public StompOrderTrackingUpdatePublisherAdapter(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishTrackingPoint(UUID customerUserId, UUID orderId, OrderTrackingPointView point) {
        messagingTemplate.convertAndSendToUser(
                customerUserId.toString(),
                "/queue/orders/" + orderId + "/tracking",
                new TrackingPointMessage(orderId, point.lat(), point.lng(), point.recordedAt())
        );
    }

    public record TrackingPointMessage(
            UUID orderId,
            BigDecimal lat,
            BigDecimal lng,
            OffsetDateTime recordedAt
    ) {
    }
}
