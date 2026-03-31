package com.foodya.backend.application.service;

import com.foodya.backend.application.port.out.SecurityPolicyPort;
import com.foodya.backend.domain.persistence.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenService {

    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_ROLE = "role";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";
    public static final String TOKEN_TYPE_RESET = "RESET";

    private final SecretKey key;
    private final SecurityPolicyPort securityPolicyPort;

    public TokenService(SecurityPolicyPort securityPolicyPort) {
        this.securityPolicyPort = securityPolicyPort;
        this.key = Keys.hmacShaKeyFor(securityPolicyPort.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(UserAccount user, String jti) {
        OffsetDateTime expiry = OffsetDateTime.now().plusMinutes(securityPolicyPort.accessTokenMinutes());
        return issue(user, TOKEN_TYPE_ACCESS, jti, expiry, Map.of(CLAIM_ROLE, user.getRole().name()));
    }

    public String issueRefreshToken(UserAccount user, String jti, String family) {
        OffsetDateTime expiry = OffsetDateTime.now().plusDays(securityPolicyPort.refreshTokenDays());
        return issue(user, TOKEN_TYPE_REFRESH, jti, expiry, Map.of("family", family));
    }

    public String issueResetToken(UserAccount user, String jti, String challengeToken) {
        OffsetDateTime expiry = OffsetDateTime.now().plusMinutes(securityPolicyPort.resetTokenMinutes());
        return issue(user, TOKEN_TYPE_RESET, jti, expiry, Map.of("challengeToken", challengeToken));
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String issue(UserAccount user,
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

    public OffsetDateTime toOffsetDateTime(Date date) {
        return date.toInstant().atOffset(ZoneOffset.UTC);
    }
}
