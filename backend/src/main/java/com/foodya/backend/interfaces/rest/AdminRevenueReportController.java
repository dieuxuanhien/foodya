package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.usecases.RevenueReportService;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.RevenueReportResponse;
import com.foodya.backend.interfaces.rest.mapper.RevenueReportRestMapper;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminRevenueReportController {

    private final RevenueReportService revenueReportService;

    public AdminRevenueReportController(RevenueReportService revenueReportService) {
        this.revenueReportService = revenueReportService;
    }

    @GetMapping("/revenue")
    @Operation(summary = "Platform revenue report")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Revenue report generated"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Validation failed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\":\"VALIDATION_FAILED\",\"message\":\"invalid date range\",\"details\":{\"from\":\"must be less than or equal to to\"},\"traceId\":\"1f2a\"}")
                    )
            )
    })
    public ResponseEntity<ApiSuccessResponse<RevenueReportResponse>> revenue(
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request
    ) {
        RevenueReportResponse data = RevenueReportRestMapper.toResponse(revenueReportService.platformRevenueReport(from, to));
        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(request)));
    }
}
