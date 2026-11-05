package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.OrderLifecycleUseCase;
import com.foodya.backend.application.dto.OrderSummaryView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.CancelOrderApiRequest;
import com.foodya.backend.interfaces.rest.dto.OrderDetailResponse;
import com.foodya.backend.interfaces.rest.dto.OrderSummaryResponse;
import com.foodya.backend.interfaces.rest.dto.OrderTrackingPointResponse;
import com.foodya.backend.interfaces.rest.dto.PageMetadata;
import com.foodya.backend.interfaces.rest.mapper.OrderLifecycleApiMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/orders")
@Tag(name = "Customer Order Lifecycle", description = "Customer order listing, detail, cancellation, and tracking")
public class CustomerOrderLifecycleController {

    private final OrderLifecycleUseCase orderLifecycleService;

    public CustomerOrderLifecycleController(OrderLifecycleUseCase orderLifecycleService) {
        this.orderLifecycleService = orderLifecycleService;
    }

    @GetMapping
    @Operation(summary = "List my orders", description = "Returns a paginated list of the authenticated customer's orders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders listed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiSuccessResponse<List<OrderSummaryResponse>>> myOrders(Authentication authentication,
                                                                   @RequestParam(required = false) @Min(0) Integer page,
                                                                   @RequestParam(required = false) @Min(1) @Max(200) Integer size,
                                                                   HttpServletRequest request) {
        PaginatedResult<OrderSummaryView> result = orderLifecycleService.customerOrders(CurrentUser.userId(authentication), page, size);
        List<OrderSummaryResponse> data = result.items().stream()
                .map(OrderLifecycleApiMapper::toSummary)
                .toList();
        return ResponseEntity.ok(ApiSuccessResponse.of(
                data,
                new PageMetadata(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(request)
        ));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get my order detail", description = "Returns full detail for one of the customer's orders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order detail returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> myOrderDetail(Authentication authentication,
                                                                 @PathVariable UUID orderId,
                                                                 HttpServletRequest request) {
        OrderDetailResponse data = OrderLifecycleApiMapper.toDetail(
                orderLifecycleService.customerOrder(CurrentUser.userId(authentication), orderId)
        );
        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(request)));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel my order", description = "Cancels an eligible order with an optional reason")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "422", description = "Order cannot be cancelled in its current state")
    })
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> cancelOrder(Authentication authentication,
                                                               @PathVariable UUID orderId,
                                                               @RequestBody(required = false) CancelOrderApiRequest cancelRequest,
                                                               HttpServletRequest request) {
        String reason = cancelRequest == null ? null : cancelRequest.reason();
        OrderDetailResponse data = OrderLifecycleApiMapper.toDetail(
                orderLifecycleService.cancelOrder(CurrentUser.userId(authentication), orderId, reason)
        );
        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(request)));
    }

    @GetMapping("/{orderId}/tracking")
    @Operation(summary = "Get order tracking points", description = "Returns recorded delivery tracking points for the customer's order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tracking points returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiSuccessResponse<List<OrderTrackingPointResponse>>> trackingPoints(Authentication authentication,
                                                                               @PathVariable UUID orderId,
                                                                               HttpServletRequest request) {
        List<OrderTrackingPointResponse> data = orderLifecycleService.customerTrackingPoints(CurrentUser.userId(authentication), orderId)
                .stream()
                .map(OrderLifecycleApiMapper::toTrackingPoint)
                .toList();
        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(request)));
    }
}
