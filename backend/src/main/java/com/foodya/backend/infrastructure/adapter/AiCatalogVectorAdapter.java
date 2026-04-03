package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.AiCatalogChunkDocument;
import com.foodya.backend.application.dto.AiCatalogVectorHit;
import com.foodya.backend.application.ports.out.AiCatalogVectorPort;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class AiCatalogVectorAdapter implements AiCatalogVectorPort {

    private static final String TABLE_NAME = "ai_catalog_chunks";

    private final JdbcTemplate jdbcTemplate;

    public AiCatalogVectorAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isReady() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.tables WHERE table_name = ?",
                    Integer.class,
                    TABLE_NAME
            );
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    @Override
    public long countChunks() {
        if (!isReady()) {
            return 0;
        }
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_catalog_chunks", Long.class);
            return count == null ? 0 : count;
        } catch (DataAccessException ex) {
            return 0;
        }
    }

    @Override
    @Transactional
    public void replaceSnapshot(List<AiCatalogChunkDocument> chunks) {
        if (!isReady()) {
            return;
        }

        try {
            jdbcTemplate.update("DELETE FROM ai_catalog_chunks");
            for (AiCatalogChunkDocument chunk : chunks) {
                jdbcTemplate.update(
                        """
                        INSERT INTO ai_catalog_chunks (id, menu_item_id, restaurant_id, chunk_text, chunk_metadata, embedding_text)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        UUID.randomUUID(),
                        chunk.menuItemId(),
                        chunk.restaurantId(),
                        chunk.chunkText(),
                        chunk.metadataJson(),
                        toVectorLiteral(chunk.embedding())
                );
            }

            if (hasVectorColumn()) {
                for (AiCatalogChunkDocument chunk : chunks) {
                    jdbcTemplate.update(
                            """
                            UPDATE ai_catalog_chunks
                            SET embedding = CAST(? AS vector), updated_at = CURRENT_TIMESTAMP
                            WHERE menu_item_id = ? AND restaurant_id = ?
                            """,
                            toVectorLiteral(chunk.embedding()),
                            chunk.menuItemId(),
                            chunk.restaurantId()
                    );
                }
            }
        } catch (DataAccessException ex) {
            // Keep AI chat flow resilient; fallback logic in service will continue without vector retrieval.
        }
    }

    @Override
    public List<AiCatalogVectorHit> searchByEmbedding(List<Double> queryEmbedding, int topK) {
        if (!isReady() || queryEmbedding == null || queryEmbedding.isEmpty()) {
            return List.of();
        }

        if (hasVectorColumn()) {
            String vectorLiteral = toVectorLiteral(queryEmbedding);
            try {
                return jdbcTemplate.query(
                        """
                        SELECT menu_item_id,
                               restaurant_id,
                               chunk_text,
                               1 - (embedding <=> CAST(? AS vector)) AS similarity
                        FROM ai_catalog_chunks
                        ORDER BY embedding <=> CAST(? AS vector)
                        LIMIT ?
                        """,
                        (rs, rowNum) -> new AiCatalogVectorHit(
                                UUID.fromString(rs.getString("menu_item_id")),
                                UUID.fromString(rs.getString("restaurant_id")),
                                rs.getString("chunk_text"),
                                rs.getDouble("similarity")
                        ),
                        vectorLiteral,
                        vectorLiteral,
                        topK
                );
            } catch (DataAccessException ex) {
                // fall through to Java cosine fallback
            }
        }

        try {
            List<AiCatalogVectorHit> scored = jdbcTemplate.query(
                    """
                    SELECT menu_item_id, restaurant_id, chunk_text, embedding_text
                    FROM ai_catalog_chunks
                    """,
                    (rs, rowNum) -> {
                        List<Double> rowEmbedding = parseVectorLiteral(rs.getString("embedding_text"));
                        double similarity = cosineSimilarity(queryEmbedding, rowEmbedding);
                        return new AiCatalogVectorHit(
                                UUID.fromString(rs.getString("menu_item_id")),
                                UUID.fromString(rs.getString("restaurant_id")),
                                rs.getString("chunk_text"),
                                similarity
                        );
                    }
            );

            return scored.stream()
                    .sorted(Comparator.comparingDouble(AiCatalogVectorHit::similarity).reversed())
                    .limit(topK)
                    .toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private boolean hasVectorColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.columns WHERE table_name = 'ai_catalog_chunks' AND column_name = 'embedding'",
                    Integer.class
            );
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private static String toVectorLiteral(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        List<String> encoded = new ArrayList<>(values.size());
        for (Double value : values) {
            encoded.add(value == null ? "0" : Double.toString(value));
        }
        return "[" + String.join(",", encoded) + "]";
    }

    private static List<Double> parseVectorLiteral(String literal) {
        if (literal == null || literal.isBlank()) {
            return List.of();
        }
        String trimmed = literal.trim();
        if (trimmed.length() < 2) {
            return List.of();
        }
        String body = trimmed.substring(1, trimmed.length() - 1);
        if (body.isBlank()) {
            return List.of();
        }
        String[] parts = body.split(",");
        List<Double> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            try {
                values.add(Double.parseDouble(part.trim()));
            } catch (NumberFormatException ex) {
                values.add(0d);
            }
        }
        return values;
    }

    private static double cosineSimilarity(List<Double> left, List<Double> right) {
        int dim = Math.min(left.size(), right.size());
        if (dim == 0) {
            return 0d;
        }

        double dot = 0d;
        double leftNorm = 0d;
        double rightNorm = 0d;
        for (int i = 0; i < dim; i++) {
            double l = left.get(i) == null ? 0d : left.get(i);
            double r = right.get(i) == null ? 0d : right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }

        if (leftNorm == 0d || rightNorm == 0d) {
            return 0d;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

}
