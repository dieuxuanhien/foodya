package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.OrderBatchingUseCase;
import com.foodya.backend.domain.entities.OrderBatch;
import com.foodya.backend.domain.value_objects.BatchCycleResult;
import com.foodya.backend.interfaces.rest.dto.*;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin batch management endpoints.
 * Route prefix: /api/v1/admin/batches
 */
@RestController
@RequestMapping("/api/v1/admin/batches")
@Tag(name = "Admin Batches", description = "Admin order batch monitoring and management")
public class AdminBatchController {

    private final OrderBatchingUseCase orderBatchingUseCase;

    public AdminBatchController(OrderBatchingUseCase orderBatchingUseCase) {
        this.orderBatchingUseCase = orderBatchingUseCase;
    }

    @Operation(summary = "List all batches", description = "Returns all batches ordered by creation date descending.")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<List<BatchApiResponse>>> listBatches(
            HttpServletRequest request) {
        List<OrderBatch> batches = orderBatchingUseCase.getAllBatches();
        List<BatchApiResponse> responses = batches.stream()
                .map(b -> {
                    var items = orderBatchingUseCase.getBatchItems(b.getId());
                    return new BatchApiResponse(
                            b.getId(), b.getDriverUserId(), b.getStatus(),
                            b.getPickupSequence(), b.getDeliverySequence(),
                            b.getTotalDistanceMeters(), b.getTotalDurationSeconds(),
                            b.getCreatedAt(), b.getStartedAt(), b.getCompletedAt(),
                            items.stream().map(i -> new BatchItemApiResponse(
                                    i.getId(), i.getOrderId(),
                                    i.getPickupIndex(), i.getDeliveryIndex(),
                                    i.getEstimatedPickupAt(), i.getEstimatedDeliveryAt(),
                                    i.getActualPickupAt(), i.getActualDeliveryAt(),
                                    i.getItemStatus())).toList());
                })
                .toList();
        return ResponseEntity.ok(ApiSuccessResponse.of(responses, RequestTrace.from(request)));
    }

    @Operation(summary = "Get batch detail", description = "Returns a specific batch by ID with all items.")
    @GetMapping("/{batchId}")
    public ResponseEntity<ApiSuccessResponse<BatchApiResponse>> getBatchDetail(
            @PathVariable UUID batchId, HttpServletRequest request) {
        OrderBatch batch = orderBatchingUseCase.getBatchDetail(batchId);
        var items = orderBatchingUseCase.getBatchItems(batchId);
        BatchApiResponse response = new BatchApiResponse(
                batch.getId(), batch.getDriverUserId(), batch.getStatus(),
                batch.getPickupSequence(), batch.getDeliverySequence(),
                batch.getTotalDistanceMeters(), batch.getTotalDurationSeconds(),
                batch.getCreatedAt(), batch.getStartedAt(), batch.getCompletedAt(),
                items.stream().map(i -> new BatchItemApiResponse(
                        i.getId(), i.getOrderId(),
                        i.getPickupIndex(), i.getDeliveryIndex(),
                        i.getEstimatedPickupAt(), i.getEstimatedDeliveryAt(),
                        i.getActualPickupAt(), i.getActualDeliveryAt(),
                        i.getItemStatus())).toList());
        return ResponseEntity.ok(ApiSuccessResponse.of(response, RequestTrace.from(request)));
    }

    @Operation(summary = "Trigger batch cycle", description = "Manually triggers a batch assignment cycle. Returns cycle statistics.")
    @PostMapping("/trigger-cycle")
    public ResponseEntity<ApiSuccessResponse<BatchCycleResultApiResponse>> triggerCycle(
            HttpServletRequest request) {
        BatchCycleResult result = orderBatchingUseCase.triggerBatchCycle();
        BatchCycleResultApiResponse response = new BatchCycleResultApiResponse(
                result.batchesCreated(), result.ordersAssigned(),
                result.singleAssigned(), result.droppedOrders(), result.solverTimeMs());
        return ResponseEntity.ok(ApiSuccessResponse.of(response, RequestTrace.from(request)));
    }
}
