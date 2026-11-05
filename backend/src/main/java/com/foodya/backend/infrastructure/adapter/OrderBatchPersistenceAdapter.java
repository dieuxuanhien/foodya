package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.OrderBatchPort;
import com.foodya.backend.domain.entities.BatchOrderItem;
import com.foodya.backend.domain.entities.OrderBatch;
import com.foodya.backend.domain.value_objects.BatchItemStatus;
import com.foodya.backend.domain.value_objects.BatchStatus;
import com.foodya.backend.infrastructure.mapper.BatchOrderItemMapper;
import com.foodya.backend.infrastructure.mapper.OrderBatchMapper;
import com.foodya.backend.infrastructure.repository.BatchOrderItemRepository;
import com.foodya.backend.infrastructure.repository.OrderBatchRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderBatchPersistenceAdapter implements OrderBatchPort {

    private final OrderBatchRepository batchRepository;
    private final BatchOrderItemRepository itemRepository;
    private final OrderBatchMapper batchMapper;
    private final BatchOrderItemMapper itemMapper;

    public OrderBatchPersistenceAdapter(OrderBatchRepository batchRepository,
                                         BatchOrderItemRepository itemRepository,
                                         OrderBatchMapper batchMapper,
                                         BatchOrderItemMapper itemMapper) {
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
    }

    @Override
    @SuppressWarnings("null")
    public OrderBatch save(OrderBatch batch) {
        var model = batchMapper.toPersistence(Objects.requireNonNull(batch));
        var saved = batchRepository.save(model);
        return batchMapper.toDomain(saved);
    }

    @Override
    public Optional<OrderBatch> findById(UUID batchId) {
        return batchRepository.findById(batchId).map(batchMapper::toDomain);
    }

    @Override
    public Optional<OrderBatch> findActiveBatchForDriver(UUID driverUserId) {
        return batchRepository.findActiveBatchForDriver(driverUserId)
                .map(batchMapper::toDomain);
    }

    @Override
    public List<OrderBatch> findAllOrderByCreatedAtDesc() {
        return batchRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(batchMapper::toDomain)
                .toList();
    }

    @Override
    @SuppressWarnings("null")
    public BatchOrderItem saveBatchItem(BatchOrderItem item) {
        var model = itemMapper.toPersistence(Objects.requireNonNull(item));
        var saved = itemRepository.save(model);
        return itemMapper.toDomain(saved);
    }

    @Override
    public List<BatchOrderItem> findBatchItemsByBatchId(UUID batchId) {
        return itemRepository.findByBatchIdOrderByPickupIndexAsc(batchId).stream()
                .map(itemMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<BatchOrderItem> findBatchItemByBatchIdAndOrderId(UUID batchId, UUID orderId) {
        return itemRepository.findByBatchIdAndOrderId(batchId, orderId)
                .map(itemMapper::toDomain);
    }

    @Override
    @SuppressWarnings("null")
    public BatchOrderItem failBatchItem(UUID batchId, UUID orderId) {
        BatchOrderItem item = findBatchItemByBatchIdAndOrderId(batchId, orderId)
                .orElseThrow(() -> new IllegalStateException("Order " + orderId + " is not part of batch " + batchId));
        item.markFailed();
        BatchOrderItem saved = saveBatchItem(item);
        refreshBatchStatus(batchId);
        return saved;
    }

    @Override
    public OrderBatch refreshBatchStatus(UUID batchId) {
        OrderBatch batch = findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Batch " + batchId + " not found"));
        if (batch.getStatus() == BatchStatus.COMPLETED || batch.getStatus() == BatchStatus.PARTIAL
                || batch.getStatus() == BatchStatus.CANCELLED) {
            return batch;
        }
        List<BatchOrderItem> items = findBatchItemsByBatchId(batchId);
        boolean allTerminal = !items.isEmpty() && items.stream().allMatch(i ->
                i.getItemStatus() == BatchItemStatus.DELIVERED || i.getItemStatus() == BatchItemStatus.FAILED);
        if (!allTerminal) {
            return batch;
        }
        boolean anyDelivered = items.stream().anyMatch(i -> i.getItemStatus() == BatchItemStatus.DELIVERED);
        boolean anyFailed = items.stream().anyMatch(i -> i.getItemStatus() == BatchItemStatus.FAILED);
        if (!anyDelivered) {
            batch.cancel(); // every order fell through: nothing was delivered
        } else {
            if (batch.getStatus() == BatchStatus.PENDING) {
                batch.activate();
            }
            if (anyFailed) {
                batch.markPartial();
            } else {
                batch.complete();
            }
        }
        return save(batch);
    }
}
