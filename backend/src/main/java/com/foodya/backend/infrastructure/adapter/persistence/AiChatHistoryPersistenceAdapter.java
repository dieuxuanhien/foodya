package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.AiChatHistoryPort;
import com.foodya.backend.domain.persistence.AiChatHistory;
import com.foodya.backend.infrastructure.repository.AiChatHistoryRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class AiChatHistoryPersistenceAdapter implements AiChatHistoryPort {

    private final AiChatHistoryRepository repository;

    public AiChatHistoryPersistenceAdapter(AiChatHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public AiChatHistory save(AiChatHistory chatHistory) {
        return repository.save(chatHistory);
    }

    @Override
    public List<AiChatHistory> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public long deleteByCreatedAtBefore(OffsetDateTime cutoff) {
        return repository.deleteByCreatedAtBefore(cutoff);
    }
}
