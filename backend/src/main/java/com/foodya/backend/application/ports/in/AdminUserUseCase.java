package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.AdminUserSummaryView;
import com.foodya.backend.application.dto.PaginatedResult;

import java.util.UUID;

public interface AdminUserUseCase {

    PaginatedResult<AdminUserSummaryView> list(String keyword, Integer page, Integer size);

    AdminUserSummaryView lock(UUID userId, UUID actorId);

    AdminUserSummaryView unlock(UUID userId, UUID actorId);

    void delete(UUID userId, UUID actorId);
}
