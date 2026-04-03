package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.ForgotPasswordResponse;
import com.foodya.backend.application.dto.LoginRequest;
import com.foodya.backend.application.dto.RegisterRequest;
import com.foodya.backend.application.dto.TokenPairResponse;
import com.foodya.backend.application.dto.VerifyOtpResponse;

import java.util.UUID;

public interface AuthUseCase {

    TokenPairResponse register(RegisterRequest request);

    TokenPairResponse login(LoginRequest request);

    TokenPairResponse refresh(String refreshToken);

    void logout(String refreshToken, UUID actorUserId);

    void logoutAll(UUID userId);

    ForgotPasswordResponse forgotPassword(String email);

    VerifyOtpResponse verifyOtp(String challengeToken, String otp);

    void resetPassword(String resetToken, String newPassword, String confirmPassword);
}
