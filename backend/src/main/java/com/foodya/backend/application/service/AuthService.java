package com.foodya.backend.application.service;

import com.foodya.backend.domain.persistence.PasswordResetChallenge;
import com.foodya.backend.domain.persistence.RefreshToken;
import com.foodya.backend.domain.persistence.UserAccount;
import com.foodya.backend.domain.model.UserRole;
import com.foodya.backend.domain.model.UserStatus;
import com.foodya.backend.application.dto.ForgotPasswordResponse;
import com.foodya.backend.application.dto.LoginRequest;
import com.foodya.backend.application.dto.RegisterRequest;
import com.foodya.backend.application.dto.TokenPairResponse;
import com.foodya.backend.application.dto.VerifyOtpResponse;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.UnauthorizedException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.port.out.PasswordResetChallengePort;
import com.foodya.backend.application.port.out.RefreshTokenPort;
import com.foodya.backend.application.port.out.SecurityPolicyPort;
import com.foodya.backend.application.port.out.UserAccountPort;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class AuthService {

    private final UserAccountPort userAccountPort;
    private final RefreshTokenPort refreshTokenPort;
    private final PasswordResetChallengePort passwordResetChallengePort;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SecurityPolicyPort securityPolicyPort;

    public AuthService(UserAccountPort userAccountPort,
                       RefreshTokenPort refreshTokenPort,
                       PasswordResetChallengePort passwordResetChallengePort,
                       AuditLogService auditLogService,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       SecurityPolicyPort securityPolicyPort) {
        this.userAccountPort = userAccountPort;
        this.refreshTokenPort = refreshTokenPort;
        this.passwordResetChallengePort = passwordResetChallengePort;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.securityPolicyPort = securityPolicyPort;
    }

    @Transactional
    public TokenPairResponse register(RegisterRequest request) {
        validateRole(request.role());
        validateUniqueness(request.username(), request.email(), request.phoneNumber());
        PasswordPolicy.validate(request.password());

        UserAccount user = new UserAccount();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPhoneNumber(normalizePhone(request.phoneNumber()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setStatus(request.role() == UserRole.MERCHANT ? UserStatus.PENDING_APPROVAL : UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        UserAccount saved = userAccountPort.save(user);
        auditLogService.securityEvent(saved.getId().toString(), "AUTH_REGISTER", "USER", saved.getId().toString(), null, "registered");
        return issueTokenPair(saved, UUID.randomUUID().toString());
    }

    @Transactional
    public TokenPairResponse login(LoginRequest request) {
        UserAccount user = userByUsernameOrEmail(request.usernameOrEmail())
                .orElseThrow(() -> new UnauthorizedException("invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("invalid credentials");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("account is not active");
        }

        auditLogService.securityEvent(user.getId().toString(), "AUTH_LOGIN", "USER", user.getId().toString(), null, "login-success");
        return issueTokenPair(user, UUID.randomUUID().toString());
    }

    @Transactional
    public TokenPairResponse refresh(String refreshToken) {
        Claims claims = parseTypedToken(refreshToken, TokenService.TOKEN_TYPE_REFRESH);
        String jti = claims.getId();
        String family = claims.get("family", String.class);
        RefreshToken existing = refreshTokenPort.findByTokenJti(jti)
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
        Claims claims = parseTypedToken(refreshToken, TokenService.TOKEN_TYPE_REFRESH);
        refreshTokenPort.findByTokenJti(claims.getId())
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
        UserAccount user = userAccountPort.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("user not found"));

        List<RefreshToken> active = refreshTokenPort.findByUserAndRevokedAtIsNullAndExpiresAtAfter(user, OffsetDateTime.now());
        OffsetDateTime now = OffsetDateTime.now();
        active.forEach(token -> token.setRevokedAt(now));
        refreshTokenPort.saveAll(active);
        auditLogService.securityEvent(userId.toString(), "AUTH_LOGOUT_ALL", "USER", userId.toString(), null, "active-sessions-revoked=" + active.size());
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(String email) {
        UserAccount user = userAccountPort.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null) {
            return new ForgotPasswordResponse("", "If this account exists, OTP has been sent");
        }

        String challengeToken = UUID.randomUUID().toString();
        String otp = generateOtp();

        PasswordResetChallenge challenge = new PasswordResetChallenge();
        challenge.setChallengeToken(challengeToken);
        challenge.setUser(user);
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setExpiresAt(OffsetDateTime.now().plusMinutes(securityPolicyPort.otpExpiryMinutes()));
        passwordResetChallengePort.save(challenge);
        auditLogService.securityEvent(user.getId().toString(), "AUTH_FORGOT_PASSWORD", "USER", user.getId().toString(), null, challengeToken);

        String hint = maskEmail(user.getEmail()) + " (dev-otp:" + otp + ")";
        return new ForgotPasswordResponse(challengeToken, hint);
    }

    @Transactional
    public VerifyOtpResponse verifyOtp(String challengeToken, String otp) {
        PasswordResetChallenge challenge = passwordResetChallengePort.findByChallengeToken(challengeToken)
                .orElseThrow(() -> new UnauthorizedException("invalid challenge token"));

        if (challenge.getConsumedAt() != null || challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("otp challenge expired");
        }
        if (!passwordEncoder.matches(otp, challenge.getOtpHash())) {
            throw new UnauthorizedException("invalid otp");
        }

        challenge.setVerifiedAt(OffsetDateTime.now());
        passwordResetChallengePort.save(challenge);
        auditLogService.securityEvent(challenge.getUser().getId().toString(), "AUTH_VERIFY_OTP", "USER", challenge.getUser().getId().toString(), null, "verified");

        String resetToken = tokenService.issueResetToken(challenge.getUser(), UUID.randomUUID().toString(), challengeToken);
        return new VerifyOtpResponse(resetToken);
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("confirmPassword does not match", Map.of("confirmPassword", "must equal newPassword"));
        }

        Claims claims = parseTypedToken(resetToken, TokenService.TOKEN_TYPE_RESET);
        String challengeToken = claims.get("challengeToken", String.class);
        PasswordResetChallenge challenge = passwordResetChallengePort.findByChallengeToken(challengeToken)
                .orElseThrow(() -> new UnauthorizedException("invalid reset challenge"));

        if (challenge.getVerifiedAt() == null || challenge.getConsumedAt() != null || challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("reset challenge is not valid");
        }

        UserAccount user = challenge.getUser();
        PasswordPolicy.validate(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ValidationException("new password must differ from current password", Map.of("newPassword", "must differ from current password"));
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccountPort.save(user);

        challenge.setConsumedAt(OffsetDateTime.now());
        passwordResetChallengePort.save(challenge);
        auditLogService.securityEvent(user.getId().toString(), "AUTH_RESET_PASSWORD", "USER", user.getId().toString(), null, "password-reset");

        logoutAll(user.getId());
    }

    private TokenPairResponse issueTokenPair(UserAccount user, String family) {
        return issueTokenPair(user, family, UUID.randomUUID().toString());
    }

    private TokenPairResponse issueTokenPair(UserAccount user, String family, String refreshJti) {
        String accessJti = UUID.randomUUID().toString();
        String accessToken = tokenService.issueAccessToken(user, accessJti);
        String refreshToken = tokenService.issueRefreshToken(user, refreshJti, family);

        Claims refreshClaims = tokenService.parseClaims(refreshToken);
        RefreshToken refresh = new RefreshToken();
        refresh.setUser(user);
        refresh.setTokenJti(refreshJti);
        refresh.setTokenFamily(family);
        refresh.setExpiresAt(tokenService.toOffsetDateTime(refreshClaims.getExpiration()));
        refreshTokenPort.save(refresh);

        return new TokenPairResponse(accessToken, refreshToken);
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

    private Optional<UserAccount> userByUsernameOrEmail(String input) {
        String trimmed = input.trim();
        if (trimmed.contains("@")) {
            return userAccountPort.findByEmail(trimmed.toLowerCase(Locale.ROOT));
        }
        return userAccountPort.findByUsername(trimmed);
    }

    private Claims parseTypedToken(String token, String expectedType) {
        Claims claims;
        try {
            claims = tokenService.parseClaims(token);
        } catch (Exception ex) {
            throw new UnauthorizedException("invalid token");
        }

        String tokenType = claims.get(TokenService.CLAIM_TOKEN_TYPE, String.class);
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
