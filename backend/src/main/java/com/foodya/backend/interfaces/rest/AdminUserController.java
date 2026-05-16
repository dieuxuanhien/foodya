package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.AdminUserSummaryView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.AdminUserUseCase;
import com.foodya.backend.interfaces.rest.dto.AdminUserResponse;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.PageMetadata;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin Users", description = "Admin governance for user lifecycle")
@Validated
public class AdminUserController {

    private final AdminUserUseCase adminUserService;

    public AdminUserController(AdminUserUseCase adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "List users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User page"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ApiSuccessResponse<List<AdminUserResponse>>> list(@RequestParam(required = false) String q,
                                                                             @RequestParam(required = false) @Min(0) Integer page,
                                                                             @RequestParam(required = false) @Min(1) @Max(200) Integer size,
                                                                             HttpServletRequest httpServletRequest) {
        PaginatedResult<AdminUserSummaryView> result = adminUserService.list(q, page, size);
        List<AdminUserResponse> data = result.items().stream().map(this::toResponse).toList();
        PageMetadata meta = new PageMetadata(result.page(), result.size(), result.totalElements(), result.totalPages());

        return ResponseEntity.ok(ApiSuccessResponse.of(data, meta, RequestTrace.from(httpServletRequest)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details")
    public ResponseEntity<ApiSuccessResponse<com.foodya.backend.interfaces.rest.dto.AdminUserDetailResponse>> getById(@PathVariable String id, HttpServletRequest httpServletRequest) {
        UUID userId = parseUuid(id, "id");
        com.foodya.backend.application.dto.AdminUserDetailView view = adminUserService.getById(userId);
        var response = new com.foodya.backend.interfaces.rest.dto.AdminUserDetailResponse(
                view.id(), view.username(), view.email(), view.phoneNumber(), view.fullName(),
                view.avatarUrl(), view.role(), view.status(), view.createdAt(), view.updatedAt()
        );
        return ResponseEntity.ok(ApiSuccessResponse.of(response, RequestTrace.from(httpServletRequest)));
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<ApiSuccessResponse<AdminUserResponse>> create(Authentication authentication,
                                                                        @Valid @RequestBody com.foodya.backend.interfaces.rest.dto.AdminUserCreateRequest request,
                                                                        HttpServletRequest httpServletRequest) {
        var command = new com.foodya.backend.application.dto.AdminUserCreateCommand(
                request.username(), request.email(), request.phoneNumber(), request.fullName(),
                request.password(), request.role(), request.status()
        );
        com.foodya.backend.application.dto.AdminUserSummaryView data = adminUserService.createUser(command, CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(data), RequestTrace.from(httpServletRequest)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public ResponseEntity<ApiSuccessResponse<AdminUserResponse>> update(Authentication authentication,
                                                                        @PathVariable String id,
                                                                        @Valid @RequestBody com.foodya.backend.interfaces.rest.dto.AdminUserUpdateRequest request,
                                                                        HttpServletRequest httpServletRequest) {
        UUID userId = parseUuid(id, "id");
        var command = new com.foodya.backend.application.dto.AdminUserUpdateCommand(
                request.username(), request.email(), request.phoneNumber(), request.fullName(),
                request.role(), request.status()
        );
        com.foodya.backend.application.dto.AdminUserSummaryView data = adminUserService.updateUser(userId, command, CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(data), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Force reset user password")
    public ResponseEntity<Void> resetPassword(Authentication authentication,
                                              @PathVariable String id,
                                              @Valid @RequestBody com.foodya.backend.interfaces.rest.dto.AdminUserResetPasswordRequest request) {
        UUID userId = parseUuid(id, "id");
        adminUserService.resetPassword(userId, request.newPassword(), CurrentUser.userId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lock")
    @Operation(summary = "Lock user account")
    public ResponseEntity<ApiSuccessResponse<AdminUserResponse>> lock(Authentication authentication,
                                                                       @PathVariable String id,
                                                                       HttpServletRequest httpServletRequest) {
        UUID userId = parseUuid(id, "id");
        AdminUserSummaryView data = adminUserService.lock(userId, CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(data), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping("/{id}/unlock")
    @Operation(summary = "Unlock user account")
    public ResponseEntity<ApiSuccessResponse<AdminUserResponse>> unlock(Authentication authentication,
                                                                         @PathVariable String id,
                                                                         HttpServletRequest httpServletRequest) {
        UUID userId = parseUuid(id, "id");
        AdminUserSummaryView data = adminUserService.unlock(userId, CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(data), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve pending user account (MERCHANT role)")
    public ResponseEntity<ApiSuccessResponse<AdminUserResponse>> approve(Authentication authentication,
                                                                         @PathVariable String id,
                                                                         HttpServletRequest httpServletRequest) {
        UUID userId = parseUuid(id, "id");
        AdminUserSummaryView data = adminUserService.approve(userId, CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(data), RequestTrace.from(httpServletRequest)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete user account")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable String id) {
        UUID userId = parseUuid(id, "id");
        adminUserService.delete(userId, CurrentUser.userId(authentication));
        return ResponseEntity.noContent().build();
    }

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid uuid", Map.of(field, "must be UUID"));
        }
    }

    private AdminUserResponse toResponse(AdminUserSummaryView view) {
        return new AdminUserResponse(
                view.id(),
                view.username(),
                view.email(),
                view.phoneNumber(),
                view.fullName(),
                view.role(),
                view.status()
        );
    }
}
