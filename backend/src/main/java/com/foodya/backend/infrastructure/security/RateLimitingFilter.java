package com.foodya.backend.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.infrastructure.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitProperties rateLimitProperties, ObjectMapper objectMapper) {
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit;
        if (path.startsWith("/api/v1/auth/")) {
            limit = rateLimitProperties.getAuthRequestsPerMinute();
        } else if (path.startsWith("/api/v1/customer/ai/")) {
            limit = rateLimitProperties.getAiRequestsPerMinute();
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientKey(request) + ":" + path;
        Instant now = Instant.now();
        WindowCounter counter = counters.compute(key, (k, old) -> {
            if (old == null || now.isAfter(old.windowEnd)) {
                return new WindowCounter(1, now.plusSeconds(60));
            }
            return new WindowCounter(old.count + 1, old.windowEnd);
        });

        if (counter.count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                SecurityErrorResponse body = new SecurityErrorResponse(
                    "RATE_LIMITED",
                    "rate limit exceeded",
                    null,
                    TraceIdHolder.from(request)
            );
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }

        if (counters.size() > 10_000) {
            counters.entrySet().removeIf(e -> now.isAfter(e.getValue().windowEnd));
        }

        filterChain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record WindowCounter(int count, Instant windowEnd) {
    }
}
