package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.AiCatalogChunkDocument;
import com.foodya.backend.application.dto.AiCatalogVectorHit;

import java.util.List;

public interface AiCatalogVectorPort {

    boolean isReady();

    long countChunks();

    void replaceSnapshot(List<AiCatalogChunkDocument> chunks);

    List<AiCatalogVectorHit> searchByEmbedding(List<Double> queryEmbedding, int topK);
}
