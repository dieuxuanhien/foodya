package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.AiCatalogChunkDocument;
import com.foodya.backend.application.dto.AiCatalogVectorHit;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface AiCatalogVectorPort {

    boolean isReady();

    long countChunks();

    Map<UUID, String> findStoredContentHashes();

    void upsertChunks(List<AiCatalogChunkDocument> chunks);

    void deleteChunksForMenuItems(Set<UUID> menuItemIds);

    List<AiCatalogVectorHit> searchByEmbedding(List<Double> queryEmbedding, int topK);

    List<AiCatalogVectorHit> searchByKeyword(String queryText, int topK);
}
