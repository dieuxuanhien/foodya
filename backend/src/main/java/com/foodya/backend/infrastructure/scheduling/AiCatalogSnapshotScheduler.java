package com.foodya.backend.infrastructure.scheduling;

import com.foodya.backend.application.ports.in.AiRecommendationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiCatalogSnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiCatalogSnapshotScheduler.class);

    private final AiRecommendationUseCase aiRecommendationUseCase;

    public AiCatalogSnapshotScheduler(AiRecommendationUseCase aiRecommendationUseCase) {
        this.aiRecommendationUseCase = aiRecommendationUseCase;
    }

    @Scheduled(
            initialDelayString = "${foodya.jobs.ai-catalog-snapshot.initial-delay-ms:0}",
            fixedDelayString = "${foodya.jobs.ai-catalog-snapshot.fixed-delay-ms:900000}"
    )
    public void refreshCatalogSnapshot() {
        try {
            aiRecommendationUseCase.refreshCatalogSnapshot();
            log.info("AI catalog snapshot refresh completed");
        } catch (Exception ex) {
            log.warn("AI catalog snapshot refresh failed", ex);
        }
    }
}
