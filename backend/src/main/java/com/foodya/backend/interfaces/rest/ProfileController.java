package com.foodya.backend.interfaces.rest;

import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.application.ports.in.ProfileUseCase;
import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.dto.ChangePasswordRequest;
import com.foodya.backend.application.dto.UpdateProfileRequest;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.ChangePasswordApiRequest;
import com.foodya.backend.interfaces.rest.dto.LocationAddressResponse;
import com.foodya.backend.interfaces.rest.dto.ProfileResponse;
import com.foodya.backend.interfaces.rest.dto.UpdateProfileApiRequest;
import com.foodya.backend.interfaces.rest.mapper.CommonApiMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Profile", description = "Authenticated profile endpoints")
@Validated
public class ProfileController {

    private final ProfileUseCase profileService;
        private final GeoPort geoPort;

        public ProfileController(ProfileUseCase profileService, GeoPort geoPort) {
        this.profileService = profileService;
                this.geoPort = geoPort;
    }

    @GetMapping
    @Operation(summary = "Get my profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current profile"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiSuccessResponse<ProfileResponse>> me(Authentication authentication,
                                                              HttpServletRequest httpServletRequest) {
        UserAccount user = profileService.me(CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(CommonApiMapper.toProfileResponse(user), RequestTrace.from(httpServletRequest)));
    }

        @GetMapping("/location-address")
        @Operation(summary = "Resolve address from location", description = "Returns Vietnamese address from latitude and longitude using Goong Maps")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Resolved address"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "422", description = "Validation failed")
        })
        public ResponseEntity<ApiSuccessResponse<LocationAddressResponse>> resolveAddress(Authentication authentication,
                                                                                                                                                                           @RequestParam @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal lat,
                                                                                                                                                                           @RequestParam @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal lng,
                                                                                                                                                                           HttpServletRequest httpServletRequest) {
                CurrentUser.userId(authentication);
                String address = geoPort.reverseGeocodeVi(lat.doubleValue(), lng.doubleValue());
                return ResponseEntity.ok(ApiSuccessResponse.of(new LocationAddressResponse(lat, lng, address), RequestTrace.from(httpServletRequest)));
        }

    @PatchMapping
    @Operation(summary = "Update my profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiSuccessResponse<ProfileResponse>> update(Authentication authentication,
                                                                  @Valid @RequestBody UpdateProfileApiRequest request,
                                                                  HttpServletRequest httpServletRequest) {
        UpdateProfileRequest command = new UpdateProfileRequest(
                request.fullName(),
                request.email(),
                request.phoneNumber(),
                request.avatarUrl()
        );
        UserAccount user = profileService.update(CurrentUser.userId(authentication), command);
        return ResponseEntity.ok(ApiSuccessResponse.of(CommonApiMapper.toProfileResponse(user), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile avatar")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar uploaded"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiSuccessResponse<ProfileResponse>> uploadAvatar(Authentication authentication,
                                                                            @RequestPart("file") MultipartFile file,
                                                                            HttpServletRequest httpServletRequest) throws java.io.IOException {
        UUID userId = CurrentUser.userId(authentication);
        UserAccount user = profileService.uploadAvatar(
                userId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
        );
        return ResponseEntity.ok(ApiSuccessResponse.of(CommonApiMapper.toProfileResponse(user), RequestTrace.from(httpServletRequest)));
    }

    @PutMapping("/password")
    @Operation(summary = "Change password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiSuccessResponse<String>> changePassword(Authentication authentication,
                                                                      @Valid @RequestBody ChangePasswordApiRequest request,
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
