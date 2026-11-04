package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.SettlementDetailView;
import com.foodya.backend.application.dto.SettlementPreview;
import com.foodya.backend.application.dto.SettlementView;
import com.foodya.backend.domain.value_objects.PayeeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Admin settlement of merchant revenue and driver earnings. Marking a settlement done never moves money;
 * it records that the transfer happened and locks those orders out of future settlements.
 */
public interface SettlementUseCase {

    /** Outstanding balance per payee. One entry per payee that has at least one unsettled order. */
    List<SettlementPreview> previews(PayeeType payeeType);

    /** Outstanding balance for one payee. */
    SettlementPreview preview(PayeeType payeeType, UUID payeeUserId);

    /**
     * Pays everything currently outstanding for the payee and returns the recorded settlement.
     *
     * @param expectedNetAmount the amount the admin saw; when given and the balance moved, nothing is settled
     */
    SettlementDetailView settle(PayeeType payeeType, UUID payeeUserId, BigDecimal expectedNetAmount,
                                String note, String actor);

    PaginatedResult<SettlementView> history(PayeeType payeeType, UUID payeeUserId, Integer page, Integer size);

    SettlementDetailView detail(UUID settlementId);
}