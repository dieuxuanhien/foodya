package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.service.AuthService;
import com.foodya.backend.application.dto.ForgotPasswordResponse;
import com.foodya.backend.application.dto.LoginRequest;
import com.foodya.backend.application.dto.RegisterRequest;
import com.foodya.backend.application.dto.TokenPairResponse;
import com.foodya.backend.application.dto.VerifyOtpResponse;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.ForgotPasswordRequest;
import com.foodya.backend.interfaces.rest.dto.LogoutRequest;
import com.foodya.backend.interfaces.rest.dto.RefreshRequest;
import com.foodya.backend.interfaces.rest.dto.ResetPasswordRequest;
import com.foodya.backend.interfaces.rest.dto.VerifyOtpRequest;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication and session lifecycle")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register account")
        @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registered"),
            @ApiResponse(responseCode = "422", description = "Validation failed"),
            @ApiResponse(responseCode = "429", description = "Rate limited")
        })
    public ResponseEntity<ApiSuccessResponse<TokenPairResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                                           HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiSuccessResponse.of(authService.register(request), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Account not active"),
            @ApiResponse(responseCode = "429", description = "Rate limited")
        })
    public ResponseEntity<ApiSuccessResponse<TokenPairResponse>> login(@Valid @RequestBody LoginRequest request,
                                                                        HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ApiSuccessResponse.of(authService.login(request), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid/reused"),
            @ApiResponse(responseCode = "429", description = "Rate limited")
        })
    public ResponseEntity<ApiSuccessResponse<TokenPairResponse>> refresh(@Valid @RequestBody RefreshRequest request,
                                                                          HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ApiSuccessResponse.of(authService.refresh(request.refreshToken()), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Start forgot password flow")
        @ApiResponses({
            @ApiResponse(responseCode = "202", description = "OTP challenge issued"),
            @ApiResponse(responseCode = "429", description = "Rate limited")
        })
    public ResponseEntity<ApiSuccessResponse<ForgotPasswordResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                                                      HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiSuccessResponse.of(authService.forgotPassword(request.email()), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping("/forgot-password/verify-otp")
    @Operation(summary = "Verify OTP")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP verified"),
            @ApiResponse(responseCode = "401", description = "Challenge/OTP invalid"),
            @ApiResponse(responseCode = "429", description = "Rate limited")
        })
    public ResponseEntity<ApiSuccessResponse<VerifyOtpResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
                                                                            HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ApiSuccessResponse.of(authService.verifyOtp(request.challengeToken(), request.otp()), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset completed"),
            @ApiResponse(responseCode = "401", description = "Reset token invalid"),
            @ApiResponse(responseCode = "422", description = "Password validation failed"),
            @ApiResponse(responseCode = "429", description = "Rate limited")
        })
    public ResponseEntity<ApiSuccessResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                                     HttpServletRequest httpServletRequest) {
        authService.resetPassword(request.resetToken(), request.newPassword(), request.confirmPassword());
        return ResponseEntity.ok(ApiSuccessResponse.of("password-reset", RequestTrace.from(httpServletRequest)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current session")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Token ownership violation"),
            @ApiResponse(responseCode = "429", description = "Rate limited")
        })
    public ResponseEntity<ApiSuccessResponse<String>> logout(Authentication authentication,
                                                              @Valid @RequestBody LogoutRequest request,
                                                              HttpServletRequest httpServletRequest) {
        authService.logout(request.refreshToken(), CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of("logged-out", RequestTrace.from(httpServletRequest)));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout all sessions")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All sessions revoked"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "429", description = "Rate limited")
        })
    public ResponseEntity<ApiSuccessResponse<String>> logoutAll(Authentication authentication,
                                                                 HttpServletRequest httpServletRequest) {
        authService.logoutAll(CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of("all-sessions-revoked", RequestTrace.from(httpServletRequest)));
    }
}
