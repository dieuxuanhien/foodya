package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.exception.ConflictException;
import com.foodya.backend.application.ports.out.SettlementPort;
import com.foodya.backend.domain.entities.Settlement;
import com.foodya.backend.domain.value_objects.PayeeType;
import com.foodya.backend.domain.value_objects.SettlementLine;
import com.foodya.backend.infrastructure.mapper.OrderMapper;
import com.foodya.backend.infrastructure.persistence.models.OrderPersistenceModel;
import com.foodya.backend.infrastructure.persistence.models.SettlementLinePersistenceModel;
import com.foodya.backend.infrastructure.persistence.models.SettlementPersistenceModel;
import com.foodya.backend.infrastructure.repository.OrderManagementRepository;
import com.foodya.backend.infrastructure.repository.SettlementLineRepository;
import com.foodya.backend.infrastructure.repository.SettlementRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Settlement persistence. The unique constraint on {@code (order_id, payee_type)} is the real guard
 * against paying an order twice; it is translated into a {@link ConflictException} so a racing admin
 * gets a clear 409 instead of a 500.
 */
@Component
public class SettlementPersistenceAdapter implements SettlementPort {

    private final SettlementRepository settlementRepository;
    private final SettlementLineRepository lineRepository;
    private final OrderManagementRepository orderRepository;
    private final OrderMapper orderMapper;

    public SettlementPersistenceAdapter(SettlementRepository settlementRepository,
                                        SettlementLineRepository lineRepository,
                                        OrderManagementRepository orderRepository,
                                        OrderMapper orderMapper) {
        this.settlementRepository = settlementRepository;
        this.lineRepository = lineRepository;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @Override
    public List<PayeeOrder> findUnsettledOrders(PayeeType payeeType) {
        return switch (payeeType) {
            case MERCHANT -> merchantOrders(orderRepository.findUnsettledMerchantOrders());
            case DRIVER -> orderRepository.findUnsettledDriverOrders().stream()
                    .map(m -> new PayeeOrder(m.getDriverUserId(), orderMapper.toDomain(m)))
                    .toList();
        };
    }

    @Override
    public List<PayeeOrder> findUnsettledOrders(PayeeType payeeType, UUID payeeUserId) {
        return switch (payeeType) {
            case MERCHANT -> merchantOrders(orderRepository.findUnsettledMerchantOrders(payeeUserId));
            case DRIVER -> orderRepository.findUnsettledDriverOrders(payeeUserId).stream()
                    .map(m -> new PayeeOrder(m.getDriverUserId(), orderMapper.toDomain(m)))
                    .toList();
        };
    }

    private List<PayeeOrder> merchantOrders(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new PayeeOrder((UUID) row[1], orderMapper.toDomain((OrderPersistenceModel) row[0])))
                .toList();
    }

    @Override
    @Transactional
    public Settlement save(Settlement settlement) {
        SettlementPersistenceModel model = new SettlementPersistenceModel();
        model.setId(settlement.getId());
        model.setPayeeType(settlement.getPayeeType());
        model.setPayeeUserId(settlement.getPayeeUserId());
        model.setDirection(settlement.getDirection());
        model.setOrderCount(settlement.getOrderCount());
        model.setGrossAmount(settlement.getGrossAmount());
        model.setPlatformFeeAmount(settlement.getPlatformFeeAmount());
        model.setCashCollectedAmount(settlement.getCashCollectedAmount());
        model.setNetAmount(settlement.getNetAmount());
        model.setNote(settlement.getNote());
        model.setSettledBy(settlement.getSettledBy());
        model.setSettledAt(settlement.getSettledAt());
        try {
            settlementRepository.saveAndFlush(model);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("one of these orders was settled by another request; reload and try again");
        }

        List<SettlementLinePersistenceModel> lines = settlement.getLines().stream()
                .map(line -> toModel(settlement, line))
                .toList();
        try {
            lineRepository.saveAllAndFlush(lines);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("one of these orders was settled by another request; reload and try again");
        }
        return settlement;
    }

    private static SettlementLinePersistenceModel toModel(Settlement settlement, SettlementLine line) {
        SettlementLinePersistenceModel m = new SettlementLinePersistenceModel();
        m.setId(UUID.randomUUID());
        m.setSettlementId(settlement.getId());
        m.setOrderId(line.orderId());
        m.setOrderCode(line.orderCode());
        m.setPayeeType(settlement.getPayeeType());
        m.setGrossAmount(line.grossAmount());
        m.setPlatformFeeAmount(line.platformFeeAmount());
        m.setCashCollectedAmount(line.cashCollectedAmount());
        m.setNetAmount(line.netAmount());
        return m;
    }

    @Override
    public Optional<Settlement> findById(UUID settlementId) {
        return settlementRepository.findById(settlementId).map(m -> toDomain(m, true));
    }

    @Override
    public PaginatedResult<Settlement> findHistory(PayeeType payeeType, UUID payeeUserId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<SettlementPersistenceModel> result;
        if (payeeType != null && payeeUserId != null) {
            result = settlementRepository.findByPayeeTypeAndPayeeUserIdOrderBySettledAtDesc(payeeType, payeeUserId, pageable);
        } else if (payeeType != null) {
            result = settlementRepository.findByPayeeTypeOrderBySettledAtDesc(payeeType, pageable);
        } else {
            result = settlementRepository.findAllByOrderBySettledAtDesc(pageable);
        }
        return new PaginatedResult<>(
                result.getContent().stream().map(m -> toDomain(m, false)).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private Settlement toDomain(SettlementPersistenceModel m, boolean withLines) {
        Settlement s = new Settlement();
        s.setId(m.getId());
        s.setPayeeType(m.getPayeeType());
        s.setPayeeUserId(m.getPayeeUserId());
        s.setDirection(m.getDirection());
        s.setOrderCount(m.getOrderCount());
        s.setGrossAmount(m.getGrossAmount());
        s.setPlatformFeeAmount(m.getPlatformFeeAmount());
        s.setCashCollectedAmount(m.getCashCollectedAmount());
        s.setNetAmount(m.getNetAmount());
        s.setNote(m.getNote());
        s.setSettledBy(m.getSettledBy());
        s.setSettledAt(m.getSettledAt());
        if (withLines) {
            s.setLines(lineRepository.findBySettlementIdOrderByOrderCodeAsc(m.getId()).stream()
                    .map(l -> new SettlementLine(l.getOrderId(), l.getOrderCode(), l.getGrossAmount(),
                            l.getPlatformFeeAmount(), l.getCashCollectedAmount(), l.getNetAmount()))
                    .toList());
        }
        return s;
    }
}