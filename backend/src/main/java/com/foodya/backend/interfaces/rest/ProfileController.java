package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.ProfileUseCase;
import com.foodya.backend.application.dto.UserAccountModel;
import com.foodya.backend.application.dto.ChangePasswordRequest;
import com.foodya.backend.application.dto.UpdateProfileRequest;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.ChangePasswordRestRequest;
import com.foodya.backend.interfaces.rest.dto.MeResponse;
import com.foodya.backend.interfaces.rest.dto.UpdateProfileRestRequest;
import com.foodya.backend.interfaces.rest.mapper.RestDtoMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Profile", description = "Authenticated profile endpoints")
public class ProfileController {

    private final ProfileUseCase profileService;

    public ProfileController(ProfileUseCase profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Get my profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current profile"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiSuccessResponse<MeResponse>> me(Authentication authentication,
                                                              HttpServletRequest httpServletRequest) {
        UserAccountModel user = profileService.me(CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(RestDtoMapper.toMeResponse(user), RequestTrace.from(httpServletRequest)));
    }

    @PatchMapping
    @Operation(summary = "Update my profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiSuccessResponse<MeResponse>> update(Authentication authentication,
                                                                  @Valid @RequestBody UpdateProfileRestRequest request,
                                                                  HttpServletRequest httpServletRequest) {
        UpdateProfileRequest command = new UpdateProfileRequest(
                request.fullName(),
                request.email(),
                request.phoneNumber(),
                request.avatarUrl()
        );
        UserAccountModel user = profileService.update(CurrentUser.userId(authentication), command);
        return ResponseEntity.ok(ApiSuccessResponse.of(RestDtoMapper.toMeResponse(user), RequestTrace.from(httpServletRequest)));
    }

    @PutMapping("/password")
    @Operation(summary = "Change password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiSuccessResponse<String>> changePassword(Authentication authentication,
                                                                      @Valid @RequestBody ChangePasswordRestRequest request,
                                                                      HttpServletRequest httpServletRequest) {
        ChangePasswordRequest command = new ChangePasswordRequest(
                request.currentPassword(),
                request.newPassword(),
                request.confirmPassword()
        );
        profileService.changePassword(CurrentUser.userId(authentication), command);
        return ResponseEntity.ok(ApiSuccessResponse.of("password-updated", RequestTrace.from(httpServletRequest)));
    }
}
