package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.PaymentUseCase;
import com.foodya.backend.interfaces.rest.dto.*;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@Tag(name = "Payments", description = "VNPAY payment initiation, IPN callback and status")
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    public PaymentController(PaymentUseCase paymentUseCase) {
        this.paymentUseCase = paymentUseCase;
    }

    @Operation(summary = "Initiate payment", description = "Creates a gateway transaction and returns the redirect URL.")
    @PostMapping("/api/v1/customer/orders/{orderId}/pay")
    public ResponseEntity<ApiSuccessResponse<PaymentApiResponses.PaymentInitiationResponse>> initiate(
            @PathVariable UUID orderId,
            @Valid @RequestBody InitiatePaymentApiRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        PaymentUseCase.PaymentInitiation result = paymentUseCase.initiatePayment(
                orderId, CurrentUser.userId(authentication), request.paymentMethod(),
                clientIp(httpRequest), request.returnUrl());
        return ResponseEntity.ok(ApiSuccessResponse.of(
                new PaymentApiResponses.PaymentInitiationResponse(result.orderId(), result.paymentUrl(),
                        result.transactionRef(), result.amount()),
                RequestTrace.from(httpRequest)));
    }

    @Operation(summary = "Payment status", description = "Returns the order payment status and latest gateway transaction status.")
    @GetMapping("/api/v1/customer/orders/{orderId}/payment-status")
    public ResponseEntity<ApiSuccessResponse<PaymentApiResponses.PaymentStatusResponse>> status(
            @PathVariable UUID orderId, Authentication authentication, HttpServletRequest httpRequest) {
        PaymentUseCase.PaymentStatusView view =
                paymentUseCase.getPaymentStatus(orderId, CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(
                new PaymentApiResponses.PaymentStatusResponse(view.orderId(), view.orderPaymentStatus(),
                        view.transactionStatus(), view.paymentMethod(), view.transactionRef()),
                RequestTrace.from(httpRequest)));
    }

    @Operation(summary = "Refund a paid order",
            description = "Admin action: refunds the most recent completed VNPAY payment for the order in full. "
                    + "Idempotent; returns refunded=false with a message when the gateway rejects it.")
    @PostMapping("/api/v1/admin/orders/{orderId}/refund")
    public ResponseEntity<ApiSuccessResponse<PaymentUseCase.PaymentRefundView>> refund(
            @PathVariable UUID orderId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String reason = body == null ? null : body.get("reason");
        PaymentUseCase.PaymentRefundView view = paymentUseCase.refundOrder(
                orderId, CurrentUser.userId(authentication).toString(), clientIp(httpRequest),
                reason == null || reason.isBlank() ? "admin refund" : reason);
        return ResponseEntity.ok(ApiSuccessResponse.of(view, RequestTrace.from(httpRequest)));
    }

    /**
     * VNPAY IPN. Must answer HTTP 200 with {@code {"RspCode":"00","Message":"Confirm Success"}} for accepted
     * notifications; any other code makes VNPAY retry.
     */
    @Operation(summary = "VNPAY IPN callback", description = "Server-to-server notification; signature-verified and idempotent.")
    @GetMapping("/api/v1/payments/vnpay/callback")
    public ResponseEntity<Map<String, String>> callback(@RequestParam Map<String, String> params) {
        PaymentUseCase.PaymentCallbackResult result = paymentUseCase.handleCallback(new HashMap<>(params));
        if (!result.verified()) {
            return ResponseEntity.ok(ipn("97", "Invalid signature"));
        }
        if (PaymentUseCase.AMOUNT_MISMATCH.equals(result.status())) {
            return ResponseEntity.ok(ipn("04", "Invalid amount"));
        }
        if (!result.success() && result.orderId() == null) {
            return ResponseEntity.ok(ipn("01", "Order not found"));
        }
        return ResponseEntity.ok(ipn("00", "Confirm Success"));
    }

    @Operation(summary = "VNPAY return URL", description = "Browser redirect target after the customer pays.")
    @GetMapping("/api/v1/payments/vnpay/return")
    public ResponseEntity<ApiSuccessResponse<Map<String, String>>> paymentReturn(
            @RequestParam Map<String, String> params, HttpServletRequest httpRequest) {
        PaymentUseCase.PaymentCallbackResult result = paymentUseCase.handleCallback(new HashMap<>(params));
        Map<String, String> body = new HashMap<>();
        body.put("status", result.success() ? "SUCCESS" : "FAILED");
        body.put("orderId", result.orderId() == null ? "" : result.orderId().toString());
        body.put("transactionRef", result.transactionRef() == null ? "" : result.transactionRef());
        body.put("responseCode", result.responseCode() == null ? "" : result.responseCode());
        return ResponseEntity.ok(ApiSuccessResponse.of(body, RequestTrace.from(httpRequest)));
    }

    private static Map<String, String> ipn(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
