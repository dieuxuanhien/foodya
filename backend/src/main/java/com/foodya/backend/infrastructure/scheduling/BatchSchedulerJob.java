package com.foodya.backend.infrastructure.scheduling;

import com.foodya.backend.application.ports.in.OrderBatchingUseCase;
import com.foodya.backend.domain.value_objects.BatchCycleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that triggers the batch assignment cycle every N seconds.
 * Default interval: 10 seconds (configurable via application.yml).
 */
@Component
public class BatchSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(BatchSchedulerJob.class);

    private final OrderBatchingUseCase orderBatchingUseCase;

    public BatchSchedulerJob(OrderBatchingUseCase orderBatchingUseCase) {
        this.orderBatchingUseCase = orderBatchingUseCase;
    }

    @Scheduled(fixedDelayString = "${foodya.batching.cycle-interval-ms:10000}")
    public void runBatchCycle() {
        try {
            BatchCycleResult result = orderBatchingUseCase.triggerBatchCycle();
            if (result.batchesCreated() > 0 || result.singleAssigned() > 0) {
                log.info("Batch cycle: {} batches ({} orders), {} single-assigned, {} dropped, solver={}ms",
                        result.batchesCreated(), result.ordersAssigned(),
                        result.singleAssigned(), result.droppedOrders(), result.solverTimeMs());
            }
        } catch (Exception ex) {
            log.error("Batch cycle failed", ex);
        }
    }
}
