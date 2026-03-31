package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.service.RevenueReportService;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.RevenueReportResponse;
import com.foodya.backend.interfaces.rest.mapper.RevenueReportRestMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/merchant/reports")
public class MerchantRevenueReportController {

    private final RevenueReportService revenueReportService;

    public MerchantRevenueReportController(RevenueReportService revenueReportService) {
        this.revenueReportService = revenueReportService;
    }

    @GetMapping("/revenue")
    @Operation(summary = "Merchant revenue report")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Revenue report generated"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Validation failed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"code\":\"VALIDATION_FAILED\",\"message\":\"invalid topItems\",\"details\":{\"topItems\":\"must be between 1 and 20\"},\"traceId\":\"1f2a\"}")
                    )
            )
    })
    public ResponseEntity<ApiSuccessResponse<RevenueReportResponse>> revenue(Authentication authentication,
                                                                              @Parameter(description = "Start date (yyyy-MM-dd)")
                                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                              @Parameter(description = "End date (yyyy-MM-dd)")
                                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                                              @Parameter(description = "Top selling items limit (1..20)")
                                                                              @RequestParam(required = false) Integer topItems,
                                                                              HttpServletRequest request) {
        RevenueReportResponse data = RevenueReportRestMapper.toResponse(
                revenueReportService.merchantRevenueReport(CurrentUser.userId(authentication), from, to, topItems)
        );
        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(request)));
    }
}
