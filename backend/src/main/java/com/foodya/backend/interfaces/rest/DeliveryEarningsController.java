package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.DriverEarningsView;
import com.foodya.backend.application.ports.in.DriverEarningsUseCase;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/delivery/earnings")
@Tag(name = "Driver Earnings", description = "What the calling driver earned from completed deliveries")
public class DeliveryEarningsController {

    private static final long DEFAULT_WINDOW_DAYS = 30;
    private static final long MAX_WINDOW_DAYS = 366;

    private final DriverEarningsUseCase driverEarningsUseCase;

    public DeliveryEarningsController(DriverEarningsUseCase driverEarningsUseCase) {
        this.driverEarningsUseCase = driverEarningsUseCase;
    }

    @Operation(summary = "My earnings",
            description = "Completed deliveries between `from` and `to` (ISO-8601, default: last 30 days). "
                    + "Each delivery credits the delivery fee minus the platform shipping margin. "
                    + "`codCashCollected` is the cash the driver collected on COD orders and still owes the platform.")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<DriverEarningsView>> earnings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Authentication authentication,
            HttpServletRequest request) {
        OffsetDateTime end = to == null ? OffsetDateTime.now() : to;
        OffsetDateTime start = from == null ? end.minusDays(DEFAULT_WINDOW_DAYS) : from;
        if (start.isAfter(end)) {
            throw new ValidationException("invalid date range", Map.of("from", "must not be after 'to'"));
        }
        if (start.plusDays(MAX_WINDOW_DAYS).isBefore(end)) {
            throw new ValidationException("date range too large",
                    Map.of("from", "range must not exceed " + MAX_WINDOW_DAYS + " days"));
        }
        DriverEarningsView view = driverEarningsUseCase.driverEarnings(CurrentUser.userId(authentication), start, end);
        return ResponseEntity.ok(ApiSuccessResponse.of(view, RequestTrace.from(request)));
    }
}