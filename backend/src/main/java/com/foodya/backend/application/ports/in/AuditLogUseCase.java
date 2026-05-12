package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.AuditLogView;
import com.foodya.backend.application.dto.PaginatedResult;

public interface AuditLogUseCase {

    PaginatedResult<AuditLogView> list(String action, String targetType, Integer page, Integer size);
}
