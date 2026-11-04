package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.Settlement;
import com.foodya.backend.domain.value_objects.PayeeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementPort {

    /** A completed order together with the user it is owed to (restaurant owner or driver). */
    record PayeeOrder(UUID payeeUserId, Order order) {}

    /** Completed orders not yet settled for the payee type, across all payees. */
    List<PayeeOrder> findUnsettledOrders(PayeeType payeeType);

    /** Completed orders not yet settled for one payee. */
    List<PayeeOrder> findUnsettledOrders(PayeeType payeeType, UUID payeeUserId);

    /**
     * Persists the settlement and its lines atomically.
     *
     * @throws com.foodya.backend.application.exception.ConflictException when one of the orders was
     *         settled by a concurrent request
     */
    Settlement save(Settlement settlement);

    Optional<Settlement> findById(UUID settlementId);

    /** Newest first. {@code payeeType} and {@code payeeUserId} are optional filters (user id needs a type). */
    PaginatedResult<Settlement> findHistory(PayeeType payeeType, UUID payeeUserId, int page, int size);
}