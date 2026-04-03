package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.AdminUserSummaryView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.AdminUserUseCase;
import com.foodya.backend.interfaces.rest.dto.AdminUserResponse;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.PageMeta;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin Users", description = "Admin governance for user lifecycle")
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
                                                                             @RequestParam(required = false) Integer page,
                                                                             @RequestParam(required = false) Integer size,
                                                                             HttpServletRequest httpServletRequest) {
        PaginatedResult<AdminUserSummaryView> result = adminUserService.list(q, page, size);
        List<AdminUserResponse> data = result.items().stream().map(this::toResponse).toList();
        PageMeta meta = new PageMeta(result.page(), result.size(), result.totalElements(), result.totalPages());

        return ResponseEntity.ok(ApiSuccessResponse.of(data, meta, RequestTrace.from(httpServletRequest)));
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Hard delete user account")
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
