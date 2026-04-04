package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.PasswordResetChallengeModel;
import com.foodya.backend.application.dto.RefreshTokenModel;
import com.foodya.backend.application.dto.TokenClaims;
import com.foodya.backend.application.dto.UserAccountModel;
import com.foodya.backend.domain.policies.PasswordPolicy;
import com.foodya.backend.domain.services.PhoneNormalizer;
import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import com.foodya.backend.application.dto.ForgotPasswordResult;
import com.foodya.backend.application.dto.LoginRequest;
import com.foodya.backend.application.dto.RegisterRequest;
import com.foodya.backend.application.dto.TokenPairResult;
import com.foodya.backend.application.dto.VerifyOtpResult;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.UnauthorizedException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.AuthUseCase;
import com.foodya.backend.application.ports.out.PasswordHashPort;
import com.foodya.backend.application.ports.out.OtpDeliveryPort;
import com.foodya.backend.application.ports.out.PasswordResetChallengePort;
import com.foodya.backend.application.ports.out.RefreshTokenPort;
import com.foodya.backend.application.ports.out.SecurityPolicyPort;
import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.application.ports.out.UserAccountPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService implements AuthUseCase {

    private final UserAccountPort userAccountPort;
    private final RefreshTokenPort refreshTokenPort;
    private final PasswordResetChallengePort passwordResetChallengePort;
    private final AuditLogService auditLogService;
    private final PasswordHashPort passwordHashPort;
    private final TokenPort tokenPort;
    private final SecurityPolicyPort securityPolicyPort;
    private final OtpDeliveryPort otpDeliveryPort;

    public AuthService(UserAccountPort userAccountPort,
                       RefreshTokenPort refreshTokenPort,
                       PasswordResetChallengePort passwordResetChallengePort,
                       AuditLogService auditLogService,
                       PasswordHashPort passwordHashPort,
                       TokenPort tokenPort,
                       SecurityPolicyPort securityPolicyPort,
                       OtpDeliveryPort otpDeliveryPort) {
        this.userAccountPort = userAccountPort;
        this.refreshTokenPort = refreshTokenPort;
        this.passwordResetChallengePort = passwordResetChallengePort;
        this.auditLogService = auditLogService;
        this.passwordHashPort = passwordHashPort;
        this.tokenPort = tokenPort;
        this.securityPolicyPort = securityPolicyPort;
        this.otpDeliveryPort = otpDeliveryPort;
    }

    @Transactional
    public TokenPairResult register(RegisterRequest request) {
        validateRole(request.role());
        validateUniqueness(request.username(), request.email(), request.phoneNumber());
        validatePassword(request.password());

        UserAccountModel user = new UserAccountModel();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPhoneNumber(normalizePhone(request.phoneNumber()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setStatus(request.role() == UserRole.MERCHANT ? UserStatus.PENDING_APPROVAL : UserStatus.ACTIVE);
        user.setPasswordHash(passwordHashPort.encode(request.password()));

        UserAccountModel saved = userAccountPort.save(user);
        auditLogService.securityEvent(saved.getId().toString(), "AUTH_REGISTER", "USER", saved.getId().toString(), null, "registered");
        return issueTokenPair(saved, UUID.randomUUID().toString());
    }

    @Transactional
    public TokenPairResult login(LoginRequest request) {
        UserAccountModel user = userByUsernameOrEmail(request.usernameOrEmail())
                .orElseThrow(() -> new UnauthorizedException("invalid credentials"));

        if (!passwordHashPort.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("invalid credentials");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("account is not active");
        }

        auditLogService.securityEvent(user.getId().toString(), "AUTH_LOGIN", "USER", user.getId().toString(), null, "login-success");
        return issueTokenPair(user, UUID.randomUUID().toString());
    }

    @Transactional
    public TokenPairResult refresh(String refreshToken) {
        TokenClaims claims = parseTypedToken(refreshToken, TokenPort.TOKEN_TYPE_REFRESH);
        String jti = claims.id();
        String family = claims.getString("family");
        RefreshTokenModel existing = refreshTokenPort.findByTokenJti(jti)
                .orElseThrow(() -> new UnauthorizedException("refresh token not recognized"));

        if (existing.getRevokedAt() != null || existing.getExpiresAt().isBefore(OffsetDateTime.now())) {
            revokeFamily(family);
            throw new UnauthorizedException("refresh token reuse or expired token detected");
        }

        existing.setRevokedAt(OffsetDateTime.now());
        String newJti = UUID.randomUUID().toString();
        existing.setReplacedByJti(newJti);
        refreshTokenPort.save(existing);
        auditLogService.securityEvent(existing.getUser().getId().toString(), "AUTH_REFRESH", "USER", existing.getUser().getId().toString(), jti, newJti);

        return issueTokenPair(existing.getUser(), family, newJti);
    }

    @Transactional
    public void logout(String refreshToken, UUID actorUserId) {
        TokenClaims claims = parseTypedToken(refreshToken, TokenPort.TOKEN_TYPE_REFRESH);
        refreshTokenPort.findByTokenJti(claims.id())
                .ifPresent(token -> {
                    if (!token.getUser().getId().equals(actorUserId)) {
                        throw new ForbiddenException("cannot revoke another user's session");
                    }
                    if (token.getRevokedAt() == null) {
                        token.setRevokedAt(OffsetDateTime.now());
                        refreshTokenPort.save(token);
                        auditLogService.securityEvent(actorUserId.toString(), "AUTH_LOGOUT", "REFRESH_TOKEN", token.getTokenJti(), null, "revoked");
                    }
                });
    }

    @Transactional
    public void logoutAll(UUID userId) {
        UserAccountModel user = userAccountPort.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("user not found"));

        List<RefreshTokenModel> active = refreshTokenPort.findByUserAndRevokedAtIsNullAndExpiresAtAfter(user, OffsetDateTime.now());
        OffsetDateTime now = OffsetDateTime.now();
        active.forEach(token -> token.setRevokedAt(now));
        refreshTokenPort.saveAll(active);
        auditLogService.securityEvent(userId.toString(), "AUTH_LOGOUT_ALL", "USER", userId.toString(), null, "active-sessions-revoked=" + active.size());
    }

    @Transactional
    public ForgotPasswordResult forgotPassword(String email) {
        UserAccountModel user = userAccountPort.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null) {
            return new ForgotPasswordResult("", "If this account exists, OTP has been sent");
        }

        String challengeToken = UUID.randomUUID().toString();
        String otp = generateOtp();

        PasswordResetChallengeModel challenge = new PasswordResetChallengeModel();
        challenge.setChallengeToken(challengeToken);
        challenge.setUser(user);
        challenge.setOtpHash(passwordHashPort.encode(otp));
        challenge.setExpiresAt(OffsetDateTime.now().plusMinutes(securityPolicyPort.otpExpiryMinutes()));
        passwordResetChallengePort.save(challenge);
        otpDeliveryPort.sendPasswordResetOtp(user.getEmail(), otp);
        auditLogService.securityEvent(user.getId().toString(), "AUTH_FORGOT_PASSWORD", "USER", user.getId().toString(), null, challengeToken);

        String hint = maskEmail(user.getEmail());
        return new ForgotPasswordResult(challengeToken, hint);
    }

    @Transactional
    public VerifyOtpResult verifyOtp(String challengeToken, String otp) {
        PasswordResetChallengeModel challenge = passwordResetChallengePort.findByChallengeToken(challengeToken)
                .orElseThrow(() -> new UnauthorizedException("invalid challenge token"));

        if (challenge.getConsumedAt() != null || challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("otp challenge expired");
        }
        if (!passwordHashPort.matches(otp, challenge.getOtpHash())) {
            throw new UnauthorizedException("invalid otp");
        }

        challenge.setVerifiedAt(OffsetDateTime.now());
        passwordResetChallengePort.save(challenge);
        auditLogService.securityEvent(challenge.getUser().getId().toString(), "AUTH_VERIFY_OTP", "USER", challenge.getUser().getId().toString(), null, "verified");

        String resetToken = tokenPort.issueResetToken(challenge.getUser(), UUID.randomUUID().toString(), challengeToken);
        return new VerifyOtpResult(resetToken);
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("confirmPassword does not match", Map.of("confirmPassword", "must equal newPassword"));
        }

        TokenClaims claims = parseTypedToken(resetToken, TokenPort.TOKEN_TYPE_RESET);
        String challengeToken = claims.getString("challengeToken");
        PasswordResetChallengeModel challenge = passwordResetChallengePort.findByChallengeToken(challengeToken)
                .orElseThrow(() -> new UnauthorizedException("invalid reset challenge"));

        if (challenge.getVerifiedAt() == null || challenge.getConsumedAt() != null || challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("reset challenge is not valid");
        }

        UserAccountModel user = challenge.getUser();
        validatePassword(newPassword);
        if (passwordHashPort.matches(newPassword, user.getPasswordHash())) {
            throw new ValidationException("new password must differ from current password", Map.of("newPassword", "must differ from current password"));
        }

        user.setPasswordHash(passwordHashPort.encode(newPassword));
        userAccountPort.save(user);

        challenge.setConsumedAt(OffsetDateTime.now());
        passwordResetChallengePort.save(challenge);
        auditLogService.securityEvent(user.getId().toString(), "AUTH_RESET_PASSWORD", "USER", user.getId().toString(), null, "password-reset");

        logoutAll(user.getId());
    }

    private TokenPairResult issueTokenPair(UserAccountModel user, String family) {
        return issueTokenPair(user, family, UUID.randomUUID().toString());
    }

    private TokenPairResult issueTokenPair(UserAccountModel user, String family, String refreshJti) {
        String accessJti = UUID.randomUUID().toString();
        String accessToken = tokenPort.issueAccessToken(user, accessJti);
        String refreshToken = tokenPort.issueRefreshToken(user, refreshJti, family);

        TokenClaims refreshClaims = tokenPort.parseClaims(refreshToken);
        RefreshTokenModel refresh = new RefreshTokenModel();
        refresh.setUser(user);
        refresh.setTokenJti(refreshJti);
        refresh.setTokenFamily(family);
        refresh.setExpiresAt(refreshClaims.expiresAt());
        refreshTokenPort.save(refresh);

        return new TokenPairResult(accessToken, refreshToken);
    }

    private void validateRole(UserRole role) {
        if (role == UserRole.ADMIN) {
            throw new ValidationException("public register cannot assign ADMIN role", Map.of("role", "ADMIN is not allowed"));
        }
    }

    private void validateUniqueness(String username, String email, String phoneNumber) {
        if (userAccountPort.existsByUsername(username.trim())) {
            throw new ValidationException("username already exists", Map.of("username", "already exists"));
        }
        if (userAccountPort.existsByEmail(email.trim().toLowerCase(Locale.ROOT))) {
            throw new ValidationException("email already exists", Map.of("email", "already exists"));
        }
        String normalizedPhone = normalizePhone(phoneNumber);
        if (userAccountPort.existsByPhoneNumber(normalizedPhone)) {
            throw new ValidationException("phoneNumber already exists", Map.of("phoneNumber", "already exists"));
        }
    }

    private Optional<UserAccountModel> userByUsernameOrEmail(String input) {
        String trimmed = input.trim();
        if (trimmed.contains("@")) {
            return userAccountPort.findByEmail(trimmed.toLowerCase(Locale.ROOT));
        }
        return userAccountPort.findByUsername(trimmed);
    }

    private TokenClaims parseTypedToken(String token, String expectedType) {
        TokenClaims claims;
        try {
            claims = tokenPort.parseClaims(token);
        } catch (Exception ex) {
            throw new UnauthorizedException("invalid token");
        }

        String tokenType = claims.getString(TokenPort.CLAIM_TOKEN_TYPE);
        if (!expectedType.equals(tokenType)) {
            throw new UnauthorizedException("invalid token type");
        }
        return claims;
    }

    private void revokeFamily(String family) {
        OffsetDateTime now = OffsetDateTime.now();
        refreshTokenPort.findByTokenFamily(family).forEach(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
                refreshTokenPort.save(token);
            }
        });
    }

    private static String normalizePhone(String phone) {
        try {
            return PhoneNormalizer.normalize(phone);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid phone number", Map.of("phoneNumber", ex.getMessage()));
        }
    }

    private static void validatePassword(String password) {
        try {
            PasswordPolicy.validate(password);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(
                    "password does not meet complexity requirements",
                    Map.of("password", ex.getMessage())
            );
        }
    }

    private static String generateOtp() {
        int code = new SecureRandom().nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
