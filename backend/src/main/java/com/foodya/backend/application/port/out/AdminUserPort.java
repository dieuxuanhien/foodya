package com.foodya.backend.application.port.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.model.OrderStatus;
import com.foodya.backend.domain.persistence.UserAccount;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AdminUserPort {

    PaginatedResult<UserAccount> search(String keyword, int page, int size);

    Optional<UserAccount> findById(UUID userId);

    UserAccount save(UserAccount userAccount);

    void delete(UserAccount userAccount);

    boolean hasCustomerOrdersInStatuses(UUID userId, Collection<OrderStatus> statuses);

    boolean hasMerchantOrdersInStatuses(UUID userId, Collection<OrderStatus> statuses);
}
