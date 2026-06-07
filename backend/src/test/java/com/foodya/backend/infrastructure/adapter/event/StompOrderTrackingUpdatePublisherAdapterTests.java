package com.foodya.backend.infrastructure.adapter.event;

import com.foodya.backend.application.dto.OrderTrackingPointView;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StompOrderTrackingUpdatePublisherAdapterTests {

    @Test
    void publishesTrackingPointToCustomerOrderQueue() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        StompOrderTrackingUpdatePublisherAdapter adapter =
                new StompOrderTrackingUpdatePublisherAdapter(messagingTemplate);
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OffsetDateTime recordedAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");

        adapter.publishTrackingPoint(
                customerId,
                orderId,
                new OrderTrackingPointView(
                        BigDecimal.valueOf(10.762622),
                        BigDecimal.valueOf(106.660172),
                        recordedAt
                )
        );

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(customerId.toString()),
                eq("/queue/orders/" + orderId + "/tracking"),
                payload.capture()
        );
        var message = assertInstanceOf(
                StompOrderTrackingUpdatePublisherAdapter.TrackingPointMessage.class,
                payload.getValue()
        );
        assertEquals(orderId, message.orderId());
        assertEquals(recordedAt, message.recordedAt());
    }
}
