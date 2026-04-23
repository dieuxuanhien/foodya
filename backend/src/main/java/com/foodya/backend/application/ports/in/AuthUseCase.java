package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.ForgotPasswordResult;
import com.foodya.backend.application.dto.LoginRequest;
import com.foodya.backend.application.dto.RegisterRequest;
import com.foodya.backend.application.dto.TokenPairResult;
import com.foodya.backend.application.dto.VerifyOtpResult;

import java.util.UUID;

public interface AuthUseCase {

    TokenPairResult register(RegisterRequest request);

    TokenPairResult login(LoginRequest request);

    TokenPairResult refresh(String refreshToken);

    void logout(String refreshToken, UUID actorUserId);

    void logoutAll(UUID userId);

    ForgotPasswordResult forgotPassword(String email);

    VerifyOtpResult verifyOtp(String challengeToken, String otp);

    void resetPassword(String resetToken, String newPassword, String confirmPassword);
}
