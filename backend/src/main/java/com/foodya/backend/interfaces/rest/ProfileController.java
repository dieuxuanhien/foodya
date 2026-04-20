package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.ProfileUseCase;
import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.dto.UserAccountData;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Profile", description = "Authenticated profile endpoints")
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
        UserAccountData user = profileService.me(CurrentUser.userId(authentication));
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
                                                                                                                                                                           @RequestParam BigDecimal lat,
                                                                                                                                                                           @RequestParam BigDecimal lng,
                                                                                                                                                                           HttpServletRequest httpServletRequest) {
                CurrentUser.userId(authentication);
                validateLatLng(lat, lng);
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
        UserAccountData user = profileService.update(CurrentUser.userId(authentication), command);
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

        private static void validateLatLng(BigDecimal lat, BigDecimal lng) {
                if (lat == null || lng == null) {
                        throw new ValidationException("invalid location", Map.of("location", "lat and lng are required"));
                }
                if (lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0) {
                        throw new ValidationException("invalid lat", Map.of("lat", "must be in [-90, 90]"));
                }
                if (lng.compareTo(BigDecimal.valueOf(-180)) < 0 || lng.compareTo(BigDecimal.valueOf(180)) > 0) {
                        throw new ValidationException("invalid lng", Map.of("lng", "must be in [-180, 180]"));
                }
        }
}
