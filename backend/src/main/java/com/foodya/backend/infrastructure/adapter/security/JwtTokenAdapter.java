package com.foodya.backend.infrastructure.adapter.security;

import com.foodya.backend.application.dto.TokenClaims;
import com.foodya.backend.application.dto.UserAccountModel;
import com.foodya.backend.application.ports.out.SecurityPolicyPort;
import com.foodya.backend.application.ports.out.TokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenAdapter implements TokenPort {

    private final SecretKey key;
    private final SecurityPolicyPort securityPolicyPort;

    public JwtTokenAdapter(SecurityPolicyPort securityPolicyPort) {
        this.securityPolicyPort = securityPolicyPort;
        this.key = Keys.hmacShaKeyFor(securityPolicyPort.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String issueAccessToken(UserAccountModel user, String jti) {
        OffsetDateTime expiry = OffsetDateTime.now().plusMinutes(securityPolicyPort.accessTokenMinutes());
        return issue(user, TOKEN_TYPE_ACCESS, jti, expiry, Map.of(CLAIM_ROLE, user.getRole().name()));
    }

    @Override
    public String issueRefreshToken(UserAccountModel user, String jti, String family) {
        OffsetDateTime expiry = OffsetDateTime.now().plusDays(securityPolicyPort.refreshTokenDays());
        return issue(user, TOKEN_TYPE_REFRESH, jti, expiry, Map.of("family", family));
    }

    @Override
    public String issueResetToken(UserAccountModel user, String jti, String challengeToken) {
        OffsetDateTime expiry = OffsetDateTime.now().plusMinutes(securityPolicyPort.resetTokenMinutes());
        return issue(user, TOKEN_TYPE_RESET, jti, expiry, Map.of("challengeToken", challengeToken));
    }

    @Override
    public TokenClaims parseClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new TokenClaims(
                claims.getSubject(),
                claims.getId(),
                claims.getExpiration().toInstant().atOffset(ZoneOffset.UTC),
                claims
        );
    }

    private String issue(UserAccountModel user,
                         String tokenType,
                         String jti,
                         OffsetDateTime expiresAt,
                         Map<String, Object> customClaims) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .id(jti == null ? UUID.randomUUID().toString() : jti)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claims(customClaims)
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt.toInstant()))
                .signWith(key)
                .compact();
    }
}