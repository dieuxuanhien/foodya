package com.foodya.backend.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;

public final class TraceIdHolder {

    public static final String TRACE_ID_ATTR = "traceId";

    private TraceIdHolder() {
    }

    public static String from(HttpServletRequest request) {
        Object traceId = request.getAttribute(TRACE_ID_ATTR);
        return traceId == null ? "n/a" : traceId.toString();
    }
}
