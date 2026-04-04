package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.NotificationLogView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.ports.in.NotificationUseCase;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.NotificationLogResponse;
import com.foodya.backend.interfaces.rest.dto.PageMetadata;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/notifications")
public class AdminNotificationController {

    private final NotificationUseCase notificationService;

    public AdminNotificationController(NotificationUseCase notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<List<NotificationLogResponse>>> list(@RequestParam(required = false) Integer page,
                                                                                   @RequestParam(required = false) Integer size,
                                                                                   HttpServletRequest request) {
        PaginatedResult<NotificationLogView> result = notificationService.list(page, size);
        List<NotificationLogResponse> data = result.items().stream().map(this::toResponse).toList();

        return ResponseEntity.ok(ApiSuccessResponse.of(
                data,
                new PageMetadata(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(request)
        ));
    }

    private NotificationLogResponse toResponse(NotificationLogView view) {
        return new NotificationLogResponse(
                view.id(),
                view.receiverUserId(),
                view.receiverType().name(),
                view.eventType(),
                view.title(),
                view.message(),
                view.status().name(),
                view.orderId(),
                view.sentAt(),
                view.readAt(),
                view.createdAt()
        );
    }
}
