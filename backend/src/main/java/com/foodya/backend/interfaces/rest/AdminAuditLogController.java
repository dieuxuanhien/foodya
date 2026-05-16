package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.AuditLogView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.ports.in.AuditLogUseCase;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.AuditLogResponse;
import com.foodya.backend.interfaces.rest.dto.PageMetadata;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@Validated
public class AdminAuditLogController {

    private final AuditLogUseCase auditLogService;

    public AdminAuditLogController(AuditLogUseCase auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<List<AuditLogResponse>>> list(@RequestParam(required = false) String eventType,
                                                                            @RequestParam(required = false) String entityType,
                                                                            @RequestParam(required = false) @Min(0) Integer page,
                                                                            @RequestParam(required = false) @Min(1) @Max(200) Integer size,
                                                                            HttpServletRequest request) {
        PaginatedResult<AuditLogView> result = auditLogService.list(eventType, entityType, page, size);
        List<AuditLogResponse> data = result.items().stream().map(this::toResponse).toList();

        return ResponseEntity.ok(ApiSuccessResponse.of(
                data,
                new PageMetadata(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(request)
        ));
    }

    private AuditLogResponse toResponse(AuditLogView view) {
        return new AuditLogResponse(
                view.id(),
                view.actorUserId(),
                view.action(),
                view.targetType(),
                view.targetId(),
                view.oldValue(),
                view.newValue(),
                null,
                view.createdAt()
        );
    }
}
