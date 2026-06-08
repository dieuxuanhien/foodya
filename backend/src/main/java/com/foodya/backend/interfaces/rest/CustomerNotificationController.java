package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.NotificationLogView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.DeviceTokenView;
import com.foodya.backend.application.dto.RegisterDeviceTokenCommand;
import com.foodya.backend.application.ports.in.NotificationUseCase;
import com.foodya.backend.domain.value_objects.DevicePlatform;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.DeviceTokenRegisterApiRequest;
import com.foodya.backend.interfaces.rest.dto.DeviceTokenResponse;
import com.foodya.backend.interfaces.rest.dto.DeviceTokenUnregisterApiRequest;
import com.foodya.backend.interfaces.rest.dto.NotificationLogResponse;
import com.foodya.backend.interfaces.rest.dto.PageMetadata;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Validated
public class CustomerNotificationController {

    private final NotificationUseCase notificationService;

    public CustomerNotificationController(NotificationUseCase notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<List<NotificationLogResponse>>> list(Authentication authentication,
                                                                                   @RequestParam(required = false) @Min(0) Integer page,
                                                                                   @RequestParam(required = false) @Min(1) @Max(200) Integer size,
                                                                                   HttpServletRequest request) {
        PaginatedResult<NotificationLogView> result = notificationService.listForUser(CurrentUser.userId(authentication), page, size);
        List<NotificationLogResponse> data = result.items().stream().map(this::toResponse).toList();
        PageMetadata meta = new PageMetadata(result.page(), result.size(), result.totalElements(), result.totalPages());
        return ResponseEntity.ok(ApiSuccessResponse.of(data, meta, RequestTrace.from(request)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiSuccessResponse<NotificationLogResponse>> markRead(Authentication authentication,
                                                                                 @PathVariable UUID id,
                                                                                 HttpServletRequest request) {
        NotificationLogView view = notificationService.markAsRead(CurrentUser.userId(authentication), id);
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(view), RequestTrace.from(request)));
    }

    @PostMapping("/devices")
    public ResponseEntity<ApiSuccessResponse<DeviceTokenResponse>> registerDevice(Authentication authentication,
                                                                                  @Valid @RequestBody DeviceTokenRegisterApiRequest body,
                                                                                  HttpServletRequest request) {
        DeviceTokenView view = notificationService.registerDevice(
                CurrentUser.userId(authentication),
                new RegisterDeviceTokenCommand(body.token(), parsePlatform(body.platform()), body.deviceId(), body.appVersion())
        );
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(view), RequestTrace.from(request)));
    }

    @DeleteMapping("/devices")
    public ResponseEntity<ApiSuccessResponse<Void>> unregisterDevice(Authentication authentication,
                                                                      @Valid @RequestBody DeviceTokenUnregisterApiRequest body,
                                                                      HttpServletRequest request) {
        notificationService.unregisterDevice(CurrentUser.userId(authentication), body.token());
        return ResponseEntity.ok(ApiSuccessResponse.of(null, RequestTrace.from(request)));
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

    private DeviceTokenResponse toResponse(DeviceTokenView view) {
        return new DeviceTokenResponse(
                view.id(),
                view.userId(),
                view.platform().name(),
                view.deviceId(),
                view.appVersion(),
                view.enabled(),
                view.lastSeenAt(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    private DevicePlatform parsePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return DevicePlatform.UNKNOWN;
        }
        try {
            return DevicePlatform.valueOf(platform.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DevicePlatform.UNKNOWN;
        }
    }
}
