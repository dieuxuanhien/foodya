package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.NotificationLogView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.ports.in.NotificationUseCase;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.NotificationLogResponse;
import com.foodya.backend.interfaces.rest.dto.PageMeta;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class CustomerNotificationController {

    private final NotificationUseCase notificationService;

    public CustomerNotificationController(NotificationUseCase notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<List<NotificationLogResponse>>> list(Authentication authentication,
                                                                                   @RequestParam(required = false) Integer page,
                                                                                   @RequestParam(required = false) Integer size,
                                                                                   HttpServletRequest request) {
        PaginatedResult<NotificationLogView> result = notificationService.listForUser(CurrentUser.userId(authentication), page, size);
        List<NotificationLogResponse> data = result.items().stream().map(this::toResponse).toList();
        PageMeta meta = new PageMeta(result.page(), result.size(), result.totalElements(), result.totalPages());
        return ResponseEntity.ok(ApiSuccessResponse.of(data, meta, RequestTrace.from(request)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiSuccessResponse<NotificationLogResponse>> markRead(Authentication authentication,
                                                                                 @PathVariable UUID id,
                                                                                 HttpServletRequest request) {
        NotificationLogView view = notificationService.markAsRead(CurrentUser.userId(authentication), id);
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(view), RequestTrace.from(request)));
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
