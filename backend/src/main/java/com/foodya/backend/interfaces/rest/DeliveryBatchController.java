package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.OrderBatchingUseCase;
import com.foodya.backend.domain.entities.BatchOrderItem;
import com.foodya.backend.domain.entities.OrderBatch;
import com.foodya.backend.interfaces.rest.dto.*;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Driver-facing batch endpoints.
 * Route prefix: /api/v1/delivery/batches
 */
@RestController
@RequestMapping("/api/v1/delivery/batches")
@Tag(name = "Delivery Batches", description = "Driver order batch management")
public class DeliveryBatchController {

    private final OrderBatchingUseCase orderBatchingUseCase;

    public DeliveryBatchController(OrderBatchingUseCase orderBatchingUseCase) {
        this.orderBatchingUseCase = orderBatchingUseCase;
    }

    @Operation(summary = "Get active batch", description = "Returns the driver's currently active batch with all order items.")
    @GetMapping("/active")
    public ResponseEntity<ApiSuccessResponse<BatchApiResponse>> getActiveBatch(
            Authentication authentication, HttpServletRequest request) {
        UUID driverUserId = CurrentUser.userId(authentication);
        OrderBatch batch = orderBatchingUseCase.getActiveBatchForDriver(driverUserId);
        List<BatchOrderItem> items = orderBatchingUseCase.getBatchItems(batch.getId());
        return ResponseEntity.ok(ApiSuccessResponse.of(toBatchResponse(batch, items), RequestTrace.from(request)));
    }

    @Operation(summary = "Get batch detail", description = "Returns a specific batch by ID.")
    @GetMapping("/{batchId}")
    public ResponseEntity<ApiSuccessResponse<BatchApiResponse>> getBatchDetail(
            @PathVariable UUID batchId, HttpServletRequest request) {
        OrderBatch batch = orderBatchingUseCase.getBatchDetail(batchId);
        List<BatchOrderItem> items = orderBatchingUseCase.getBatchItems(batchId);
        return ResponseEntity.ok(ApiSuccessResponse.of(toBatchResponse(batch, items), RequestTrace.from(request)));
    }

    @Operation(summary = "Mark pickup complete", description = "Driver marks an order as picked up from restaurant.")
    @PostMapping("/{batchId}/orders/{orderId}/pickup")
    public ResponseEntity<ApiSuccessResponse<BatchItemApiResponse>> markPickup(
            @PathVariable UUID batchId, @PathVariable UUID orderId,
            Authentication authentication, HttpServletRequest request) {
        UUID driverUserId = CurrentUser.userId(authentication);
        BatchOrderItem item = orderBatchingUseCase.markPickupComplete(batchId, orderId, driverUserId);
        return ResponseEntity.ok(ApiSuccessResponse.of(toItemResponse(item), RequestTrace.from(request)));
    }

    @Operation(summary = "Mark delivery complete", description = "Driver marks an order as delivered to customer.")
    @PostMapping("/{batchId}/orders/{orderId}/delivery")
    public ResponseEntity<ApiSuccessResponse<BatchItemApiResponse>> markDelivery(
            @PathVariable UUID batchId, @PathVariable UUID orderId,
            Authentication authentication, HttpServletRequest request) {
        UUID driverUserId = CurrentUser.userId(authentication);
        BatchOrderItem item = orderBatchingUseCase.markDeliveryComplete(batchId, orderId, driverUserId);
        return ResponseEntity.ok(ApiSuccessResponse.of(toItemResponse(item), RequestTrace.from(request)));
    }

    private BatchApiResponse toBatchResponse(OrderBatch batch, List<BatchOrderItem> items) {
        return new BatchApiResponse(
                batch.getId(), batch.getDriverUserId(), batch.getStatus(),
                batch.getPickupSequence(), batch.getDeliverySequence(),
                batch.getTotalDistanceMeters(), batch.getTotalDurationSeconds(),
                batch.getCreatedAt(), batch.getStartedAt(), batch.getCompletedAt(),
                items.stream().map(this::toItemResponse).toList());
    }

    private BatchItemApiResponse toItemResponse(BatchOrderItem item) {
        return new BatchItemApiResponse(
                item.getId(), item.getOrderId(),
                item.getPickupIndex(), item.getDeliveryIndex(),
                item.getEstimatedPickupAt(), item.getEstimatedDeliveryAt(),
                item.getActualPickupAt(), item.getActualDeliveryAt(),
                item.getItemStatus());
    }
}
