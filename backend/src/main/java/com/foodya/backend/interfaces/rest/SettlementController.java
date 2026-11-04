package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.SettlementDetailView;
import com.foodya.backend.application.dto.SettlementPreview;
import com.foodya.backend.application.dto.SettlementView;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.SettlementUseCase;
import com.foodya.backend.domain.value_objects.PayeeType;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.PageMetadata;
import com.foodya.backend.interfaces.rest.dto.SettleSettlementApiRequest;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Settlement of merchant revenue and driver earnings.
 *
 * <p>Money rules (all from amounts snapshotted on each order at checkout):
 * <ul>
 *   <li>Merchant gets {@code subtotal - commission} (commission is {@code finance.commission_rate_percent}, default 15%).</li>
 *   <li>Driver earns {@code deliveryFee - shippingMargin}. For a COD order the driver also holds the full order
 *       total in cash, which is subtracted: a positive result is a PAYOUT to the driver, a negative one a
 *       COLLECTION the driver hands to the platform.</li>
 *   <li>Marking a settlement done moves no money; it records the transfer and excludes those orders from
 *       the next balance.</li>
 * </ul>
 */
@RestController
@Validated
@Tag(name = "Settlements", description = "Merchant payouts, driver earnings and COD cash collection: balances, Done, history")
public class SettlementController {

    private final SettlementUseCase settlementUseCase;

    public SettlementController(SettlementUseCase settlementUseCase) {
        this.settlementUseCase = settlementUseCase;
    }

    // ------------------------------------------------------------------ admin

    @Operation(summary = "Outstanding balances",
            description = "Everyone of the given type (MERCHANT or DRIVER) with unsettled completed orders, and the amount "
                    + "due. direction=PAYOUT means the platform pays them, COLLECTION means they pay the platform.")
    @GetMapping("/api/v1/admin/settlements/pending")
    public ResponseEntity<ApiSuccessResponse<List<SettlementPreview>>> pending(
            @RequestParam String payeeType, HttpServletRequest request) {
        return ok(settlementUseCase.previews(parsePayeeType(payeeType)), request);
    }

    @Operation(summary = "Outstanding balance of one payee",
            description = "Amount currently due to or from one merchant owner or driver (zero when nothing is outstanding).")
    @GetMapping("/api/v1/admin/settlements/pending/{payeeType}/{payeeUserId}")
    public ResponseEntity<ApiSuccessResponse<SettlementPreview>> pendingOf(
            @PathVariable String payeeType, @PathVariable String payeeUserId, HttpServletRequest request) {
        return ok(settlementUseCase.preview(parsePayeeType(payeeType), parseUuid(payeeUserId, "payeeUserId")), request);
    }

    @Operation(summary = "Mark a payee's balance as done",
            description = "The Done click. Settles everything outstanding for the payee and records it in history. "
                    + "Send expectedNetAmount (the amount that was on screen) to be rejected with 409 if the balance moved. "
                    + "Returns 422 when there is nothing to settle.")
    @PostMapping("/api/v1/admin/settlements/{payeeType}/{payeeUserId}/settle")
    public ResponseEntity<ApiSuccessResponse<SettlementDetailView>> settle(
            @PathVariable String payeeType, @PathVariable String payeeUserId,
            @Valid @RequestBody(required = false) SettleSettlementApiRequest body,
            Authentication authentication, HttpServletRequest request) {
        SettlementDetailView settled = settlementUseCase.settle(
                parsePayeeType(payeeType), parseUuid(payeeUserId, "payeeUserId"),
                body == null ? null : body.expectedNetAmount(),
                body == null ? null : body.note(),
                CurrentUser.userId(authentication).toString());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(settled, RequestTrace.from(request)));
    }

    @Operation(summary = "Settlement history",
            description = "Every recorded settlement, newest first. Filter by payeeType, and by payeeUserId together with payeeType.")
    @GetMapping("/api/v1/admin/settlements")
    public ResponseEntity<ApiSuccessResponse<List<SettlementView>>> history(
            @RequestParam(required = false) String payeeType,
            @RequestParam(required = false) String payeeUserId,
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) @Max(200) Integer size,
            HttpServletRequest request) {
        PaginatedResult<SettlementView> result = settlementUseCase.history(
                payeeType == null || payeeType.isBlank() ? null : parsePayeeType(payeeType),
                payeeUserId == null || payeeUserId.isBlank() ? null : parseUuid(payeeUserId, "payeeUserId"),
                page, size);
        return paged(result, request);
    }

    @Operation(summary = "Settlement detail", description = "One recorded settlement with every order it covered.")
    @GetMapping("/api/v1/admin/settlements/{settlementId}")
    public ResponseEntity<ApiSuccessResponse<SettlementDetailView>> detail(
            @PathVariable String settlementId, HttpServletRequest request) {
        return ok(settlementUseCase.detail(parseUuid(settlementId, "settlementId")), request);
    }

    // --------------------------------------------------------------- merchant

    @Operation(summary = "My outstanding payout (merchant)",
            description = "Revenue not yet paid out to the calling restaurant owner, across all their restaurants.")
    @GetMapping("/api/v1/merchant/settlements/pending")
    public ResponseEntity<ApiSuccessResponse<SettlementPreview>> merchantPending(
            Authentication authentication, HttpServletRequest request) {
        return ok(settlementUseCase.preview(PayeeType.MERCHANT, CurrentUser.userId(authentication)), request);
    }

    @Operation(summary = "My payout history (merchant)", description = "Payouts recorded for the calling restaurant owner, newest first.")
    @GetMapping("/api/v1/merchant/settlements")
    public ResponseEntity<ApiSuccessResponse<List<SettlementView>>> merchantHistory(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) @Max(200) Integer size,
            Authentication authentication, HttpServletRequest request) {
        return paged(settlementUseCase.history(PayeeType.MERCHANT, CurrentUser.userId(authentication), page, size), request);
    }

    // ----------------------------------------------------------------- driver

    @Operation(summary = "My outstanding balance (driver)",
            description = "Earnings minus COD cash held. PAYOUT: the platform owes the driver. COLLECTION: the driver owes the "
                    + "platform the netAmount.")
    @GetMapping("/api/v1/delivery/settlements/pending")
    public ResponseEntity<ApiSuccessResponse<SettlementPreview>> driverPending(
            Authentication authentication, HttpServletRequest request) {
        return ok(settlementUseCase.preview(PayeeType.DRIVER, CurrentUser.userId(authentication)), request);
    }

    @Operation(summary = "My settlement history (driver)", description = "Payouts and cash collections recorded for the calling driver.")
    @GetMapping("/api/v1/delivery/settlements")
    public ResponseEntity<ApiSuccessResponse<List<SettlementView>>> driverHistory(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) @Max(200) Integer size,
            Authentication authentication, HttpServletRequest request) {
        return paged(settlementUseCase.history(PayeeType.DRIVER, CurrentUser.userId(authentication), page, size), request);
    }

    // ---------------------------------------------------------------- helpers

    private static <T> ResponseEntity<ApiSuccessResponse<T>> ok(T data, HttpServletRequest request) {
        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(request)));
    }

    private static ResponseEntity<ApiSuccessResponse<List<SettlementView>>> paged(
            PaginatedResult<SettlementView> result, HttpServletRequest request) {
        return ResponseEntity.ok(ApiSuccessResponse.of(result.items(),
                new PageMetadata(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(request)));
    }

    private static PayeeType parsePayeeType(String raw) {
        try {
            return PayeeType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ValidationException("invalid payeeType", Map.of("payeeType", "must be MERCHANT or DRIVER"));
        }
    }

    private static UUID parseUuid(String raw, String field) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid " + field, Map.of(field, "must be a valid UUID"));
        }
    }
}