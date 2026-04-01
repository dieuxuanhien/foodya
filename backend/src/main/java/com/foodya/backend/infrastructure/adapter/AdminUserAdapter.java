package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.ports.out.AdminUserPort;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.repository.AdminUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class AdminUserAdapter implements AdminUserPort {

    private final AdminUserRepository repository;

    public AdminUserAdapter(AdminUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaginatedResult<UserAccount> search(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserAccount> result;
        if (keyword == null || keyword.isBlank()) {
            result = repository.findAll(pageable);
        } else {
            String normalized = keyword.trim();
            result = repository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrPhoneNumberContainingIgnoreCase(
                    normalized,
                    normalized,
                    normalized,
                    normalized,
                    pageable
            );
        }

        return new PaginatedResult<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public Optional<UserAccount> findById(UUID userId) {
        return repository.findById(Objects.requireNonNull(userId));
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        return repository.save(Objects.requireNonNull(userAccount));
    }

    @Override
    public void delete(UserAccount userAccount) {
        repository.delete(Objects.requireNonNull(userAccount));
    }

    @Override
    public boolean hasCustomerOrdersInStatuses(UUID userId, Collection<OrderStatus> statuses) {
        return repository.hasCustomerOrdersInStatuses(Objects.requireNonNull(userId), Objects.requireNonNull(statuses));
    }

    @Override
    public boolean hasMerchantOrdersInStatuses(UUID userId, Collection<OrderStatus> statuses) {
        return repository.hasMerchantOrdersInStatuses(Objects.requireNonNull(userId), Objects.requireNonNull(statuses));
    }
}
