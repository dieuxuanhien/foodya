package com.foodya.backend.interfaces.rest.support;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestTrace {

    private static final String TRACE_ID_ATTR = "traceId";

    private RequestTrace() {
    }

    public static String from(HttpServletRequest request) {
        Object traceId = request.getAttribute(TRACE_ID_ATTR);
        return traceId == null ? "n/a" : traceId.toString();
    }
}