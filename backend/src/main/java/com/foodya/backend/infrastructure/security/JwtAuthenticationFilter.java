package com.foodya.backend.infrastructure.security;

import com.foodya.backend.application.dto.TokenClaims;
import com.foodya.backend.application.ports.out.TokenPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenPort tokenPort;

    public JwtAuthenticationFilter(TokenPort tokenPort) {
        this.tokenPort = tokenPort;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            TokenClaims claims = tokenPort.parseClaims(token);
            if (!TokenPort.TOKEN_TYPE_ACCESS.equals(claims.getString(TokenPort.CLAIM_TOKEN_TYPE))) {
                filterChain.doFilter(request, response);
                return;
            }

            AuthPrincipal principal = new AuthPrincipal(
                    UUID.fromString(claims.subject()),
                    claims.getString(TokenPort.CLAIM_ROLE)
            );

            String role = claims.getString(TokenPort.CLAIM_ROLE);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
