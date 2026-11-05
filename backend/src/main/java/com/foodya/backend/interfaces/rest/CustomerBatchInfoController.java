package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.OrderBatchingUseCase;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.CustomerBatchInfoApiResponse;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Customer-facing batch info endpoint.
 * Route prefix: /api/v1/customer/orders/{orderId}/batch-info
 */
@RestController
@RequestMapping("/api/v1/customer/orders")
@Tag(name = "Customer Batch Info", description = "Customer order batching information")
public class CustomerBatchInfoController {

    private final OrderBatchingUseCase orderBatchingUseCase;

    public CustomerBatchInfoController(OrderBatchingUseCase orderBatchingUseCase) {
        this.orderBatchingUseCase = orderBatchingUseCase;
    }

    @Operation(summary = "Get batch info for order",
            description = "Returns batch info for a customer's order including ETA, position in delivery sequence, and total orders in batch.")
    @GetMapping("/{orderId}/batch-info")
    public ResponseEntity<ApiSuccessResponse<CustomerBatchInfoApiResponse>> getBatchInfo(
            @PathVariable UUID orderId,
            Authentication authentication,
            HttpServletRequest request) {
        UUID customerUserId = CurrentUser.userId(authentication);
        OrderBatchingUseCase.CustomerBatchInfo info =
                orderBatchingUseCase.getCustomerBatchInfo(orderId, customerUserId);
        CustomerBatchInfoApiResponse response = new CustomerBatchInfoApiResponse(
                info.batchId(), info.totalOrdersInBatch(), info.myDeliveryPosition(),
                info.estimatedDeliveryAt(), info.estimatedMinutesRemaining());
        return ResponseEntity.ok(ApiSuccessResponse.of(response, RequestTrace.from(request)));
    }
}
