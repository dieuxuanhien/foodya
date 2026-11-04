package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.SettlementDetailView;
import com.foodya.backend.application.dto.SettlementPreview;
import com.foodya.backend.application.dto.SettlementView;
import com.foodya.backend.application.exception.ConflictException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.SettlementUseCase;
import com.foodya.backend.application.ports.out.SettlementPort;
import com.foodya.backend.application.ports.out.UserAccountPort;
import com.foodya.backend.application.support.PaginationPolicy;
import com.foodya.backend.domain.entities.Settlement;
import com.foodya.backend.domain.policies.SettlementPolicy;
import com.foodya.backend.domain.value_objects.PayeeType;
import com.foodya.backend.domain.value_objects.SettlementLine;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-side settlement of merchant revenue and driver earnings.
 *
 * <p>The preview never trusts a running balance: it is recomputed from the orders that have no settlement
 * line yet, using the fee amounts snapshotted on each order. Settling writes one immutable record, so the
 * history is a ledger rather than a mutable counter.
 */
public class SettlementService implements SettlementUseCase {

    private final SettlementPort settlementPort;
    private final UserAccountPort userAccountPort;
    private final AuditLogService auditLogService;
    private final PaginationPolicy paginationPolicy;

    public SettlementService(SettlementPort settlementPort,
                             UserAccountPort userAccountPort,
                             AuditLogService auditLogService,
                             PaginationPolicy paginationPolicy) {
        this.settlementPort = settlementPort;
        this.userAccountPort = userAccountPort;
        this.auditLogService = auditLogService;
        this.paginationPolicy = paginationPolicy;
    }

    @Override
    public List<SettlementPreview> previews(PayeeType payeeType) {
        Map<UUID, List<SettlementLine>> grouped = new LinkedHashMap<>();
        for (SettlementPort.PayeeOrder po : settlementPort.findUnsettledOrders(payeeType)) {
            if (!SettlementPolicy.isPayable(po.order(), payeeType)) {
                continue;
            }
            grouped.computeIfAbsent(po.payeeUserId(), k -> new java.util.ArrayList<>())
                    .add(SettlementPolicy.lineFor(po.order(), payeeType));
        }
        return grouped.entrySet().stream()
                .map(e -> preview(payeeType, e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    public SettlementPreview preview(PayeeType payeeType, UUID payeeUserId) {
        List<SettlementLine> lines = settlementPort.findUnsettledOrders(payeeType, payeeUserId).stream()
                .filter(po -> SettlementPolicy.isPayable(po.order(), payeeType))
                .map(po -> SettlementPolicy.lineFor(po.order(), payeeType))
                .toList();
        return preview(payeeType, payeeUserId, lines);
    }

    @Override
    public SettlementDetailView settle(PayeeType payeeType, UUID payeeUserId, BigDecimal expectedNetAmount,
                                       String note, String actor) {
        List<SettlementLine> lines = settlementPort.findUnsettledOrders(payeeType, payeeUserId).stream()
                .filter(po -> SettlementPolicy.isPayable(po.order(), payeeType))
                .map(po -> SettlementPolicy.lineFor(po.order(), payeeType))
                .toList();
        if (lines.isEmpty()) {
            throw new ValidationException("nothing to settle for this payee",
                    Map.of("payeeUserId", "no unsettled completed orders"));
        }

        BigDecimal currentNet = SettlementPolicy.totals(lines).netAmount();
        if (expectedNetAmount != null && expectedNetAmount.compareTo(currentNet) != 0) {
            throw new ConflictException("balance changed to " + currentNet.toPlainString()
                    + " since it was loaded; review it and confirm again");
        }

        Settlement settlement = Settlement.create(payeeType, payeeUserId, lines, note, actor);
        Settlement saved = settlementPort.save(settlement);

        auditLogService.securityEvent(actor, "ADMIN_SETTLEMENT_" + saved.getDirection().name(),
                payeeType.name(), payeeUserId.toString(), null,
                saved.getDirection().name() + " " + saved.getNetAmount().toPlainString()
                        + " for " + saved.getOrderCount() + " orders");

        return toDetail(saved, payeeType);
    }

    @Override
    public PaginatedResult<SettlementView> history(PayeeType payeeType, UUID payeeUserId, Integer page, Integer size) {
        if (payeeUserId != null && payeeType == null) {
            throw new ValidationException("payeeType is required when payeeUserId is given",
                    Map.of("payeeType", "must be MERCHANT or DRIVER"));
        }
        PaginationPolicy.PaginationSpec spec = paginationPolicy.page(page, size);
        PaginatedResult<Settlement> result = settlementPort.findHistory(payeeType, payeeUserId, spec.page(), spec.size());
        return new PaginatedResult<>(
                result.items().stream().map(s -> toView(s, s.getPayeeType())).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @Override
    public SettlementDetailView detail(UUID settlementId) {
        Settlement settlement = settlementPort.findById(settlementId)
                .orElseThrow(() -> new NotFoundException("settlement not found"));
        return toDetail(settlement, settlement.getPayeeType());
    }

    private SettlementPreview preview(PayeeType payeeType, UUID payeeUserId, List<SettlementLine> lines) {
        SettlementPolicy.Totals totals = SettlementPolicy.totals(lines);
        return new SettlementPreview(payeeType, payeeUserId, payeeName(payeeType, payeeUserId),
                totals.direction(), totals.orderCount(), totals.grossAmount(), totals.platformFeeAmount(),
                totals.cashCollectedAmount(), totals.netAmount());
    }

    private SettlementDetailView toDetail(Settlement s, PayeeType payeeType) {
        List<SettlementDetailView.Line> lines = s.getLines().stream()
                .map(l -> new SettlementDetailView.Line(l.orderId(), l.orderCode(), l.grossAmount(),
                        l.platformFeeAmount(), l.cashCollectedAmount(), l.netAmount()))
                .toList();
        return new SettlementDetailView(toView(s, payeeType), lines);
    }

    private SettlementView toView(Settlement s, PayeeType payeeType) {
        return new SettlementView(s.getId(), s.getPayeeType(), s.getPayeeUserId(),
                payeeName(payeeType, s.getPayeeUserId()), s.getDirection(), s.getOrderCount(),
                s.getGrossAmount(), s.getPlatformFeeAmount(), s.getCashCollectedAmount(), s.getNetAmount(),
                s.getNote(), s.getSettledBy(), s.getSettledAt());
    }

    /** Display name for the payee; falls back to the id so a settlement is never unidentifiable. */
    private String payeeName(PayeeType payeeType, UUID payeeUserId) {
        String name = userAccountPort.findById(payeeUserId).map(u -> u.getFullName()).orElse(null);
        return name == null || name.isBlank() ? payeeUserId.toString() : name;
    }
}
