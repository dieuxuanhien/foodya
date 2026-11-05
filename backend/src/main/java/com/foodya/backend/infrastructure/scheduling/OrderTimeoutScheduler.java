package com.foodya.backend.infrastructure.scheduling;

import com.foodya.backend.application.ports.in.PaymentUseCase;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.application.usecases.OrderClosureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Safety-net jobs for states that would otherwise stay stuck forever: unchecked orders, abandoned
 * driver sessions, unconfirmed payments and refunds that failed on the first attempt.
 *
 * <p>All thresholds are runtime system parameters; 0 disables the corresponding sweep.
 */
@Component
public class OrderTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    private final OrderClosureService orderClosureService;
    private final PaymentUseCase paymentUseCase;
    private final SystemParameterPort systemParameterPort;

    public OrderTimeoutScheduler(OrderClosureService orderClosureService,
                                 PaymentUseCase paymentUseCase,
                                 SystemParameterPort systemParameterPort) {
        this.orderClosureService = orderClosureService;
        this.paymentUseCase = paymentUseCase;
        this.systemParameterPort = systemParameterPort;
    }

    @Scheduled(fixedDelayString = "${foodya.jobs.order-timeouts.delay-ms:60000}")
    public void sweep() {
        try {
            int cancelled = orderClosureService.cancelTimedOutOrders();
            int expiredSessions = orderClosureService.expireStaleDriverSessions();
            int expiredPayments = paymentUseCase.expireStalePayments(
                    Duration.ofMinutes(longParam("payment.pending_expiry_minutes", 30)));
            int refunded = paymentUseCase.retryPendingRefunds();
            if (cancelled + expiredSessions + expiredPayments + refunded > 0) {
                log.info("Order timeout sweep: cancelled={}, expiredDriverSessions={}, expiredPayments={}, refunded={}",
                        cancelled, expiredSessions, expiredPayments, refunded);
            }
        } catch (Exception ex) {
            log.error("Order timeout sweep failed", ex);
        }
    }

    private long longParam(String key, long defaultValue) {
        return systemParameterPort.findById(key)
                .map(p -> {
                    try {
                        return Long.parseLong(p.getValue().trim());
                    } catch (NumberFormatException ex) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }
}