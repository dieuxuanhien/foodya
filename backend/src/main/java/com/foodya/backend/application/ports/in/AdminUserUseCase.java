package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.AdminUserDetailView;
import com.foodya.backend.application.dto.AdminUserCreateCommand;
import com.foodya.backend.application.dto.AdminUserUpdateCommand;
import com.foodya.backend.application.dto.AdminUserSummaryView;
import com.foodya.backend.application.dto.PaginatedResult;

import java.util.UUID;

public interface AdminUserUseCase {

    PaginatedResult<AdminUserSummaryView> list(String keyword, Integer page, Integer size);

    AdminUserSummaryView lock(UUID userId, UUID actorId);

    AdminUserSummaryView unlock(UUID userId, UUID actorId);

    void delete(UUID userId, UUID actorId);

    AdminUserDetailView getById(UUID userId);

    AdminUserSummaryView createUser(AdminUserCreateCommand command, UUID actorId);

    AdminUserSummaryView updateUser(UUID userId, AdminUserUpdateCommand command, UUID actorId);

    void resetPassword(UUID userId, String newPassword, UUID actorId);
}
