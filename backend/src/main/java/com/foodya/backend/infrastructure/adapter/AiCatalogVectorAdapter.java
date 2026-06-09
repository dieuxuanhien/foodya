package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.AiCatalogChunkDocument;
import com.foodya.backend.application.dto.AiCatalogVectorHit;
import com.foodya.backend.application.ports.out.AiCatalogVectorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AiCatalogVectorAdapter implements AiCatalogVectorPort {

    private static final Logger log = LoggerFactory.getLogger(AiCatalogVectorAdapter.class);

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
    public Map<UUID, String> findStoredContentHashes() {
        if (!isReady()) {
            return Map.of();
        }

        try {
            List<Object[]> rows = jdbcTemplate.query(
                    "SELECT menu_item_id, content_hash FROM ai_catalog_chunks",
                    (rs, rowNum) -> new Object[]{
                            UUID.fromString(rs.getString("menu_item_id")),
                            rs.getString("content_hash")
                    }
            );
            Map<UUID, String> hashesByMenuItem = new HashMap<>();
            for (Object[] row : rows) {
                hashesByMenuItem.put((UUID) row[0], (String) row[1]);
            }
            return hashesByMenuItem;
        } catch (DataAccessException ex) {
            log.warn("Failed to read stored AI catalog content hashes; treating catalog as fully dirty this cycle", ex);
            return Map.of();
        }
    }

    @Override
    @Transactional
    public void upsertChunks(List<AiCatalogChunkDocument> chunks) {
        if (!isReady() || chunks == null || chunks.isEmpty()) {
            return;
        }

        try {
            for (AiCatalogChunkDocument chunk : chunks) {
                jdbcTemplate.update(
                        """
                        INSERT INTO ai_catalog_chunks (id, menu_item_id, restaurant_id, chunk_text, chunk_metadata, embedding_text, content_hash)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (menu_item_id) DO UPDATE
                        SET restaurant_id = EXCLUDED.restaurant_id,
                            chunk_text = EXCLUDED.chunk_text,
                            chunk_metadata = EXCLUDED.chunk_metadata,
                            embedding_text = EXCLUDED.embedding_text,
                            content_hash = EXCLUDED.content_hash,
                            updated_at = CURRENT_TIMESTAMP
                        """,
                        UUID.randomUUID(),
                        chunk.menuItemId(),
                        chunk.restaurantId(),
                        chunk.chunkText(),
                        chunk.metadataJson(),
                        toVectorLiteral(chunk.embedding()),
                        chunk.contentHash()
                );
            }

            if (hasVectorColumn()) {
                for (AiCatalogChunkDocument chunk : chunks) {
                    jdbcTemplate.update(
                            """
                            UPDATE ai_catalog_chunks
                            SET embedding = CAST(? AS vector), updated_at = CURRENT_TIMESTAMP
                            WHERE menu_item_id = ?
                            """,
                            toVectorLiteral(chunk.embedding()),
                            chunk.menuItemId()
                    );
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Failed to upsert {} AI catalog chunk(s); they will be retried next sync cycle", chunks.size(), ex);
        }
    }

    @Override
    @Transactional
    public void deleteChunksForMenuItems(Set<UUID> menuItemIds) {
        if (!isReady() || menuItemIds == null || menuItemIds.isEmpty()) {
            return;
        }

        try {
            String placeholders = menuItemIds.stream().map(id -> "?").collect(Collectors.joining(","));
            jdbcTemplate.update(
                    "DELETE FROM ai_catalog_chunks WHERE menu_item_id IN (" + placeholders + ")",
                    menuItemIds.toArray()
            );
        } catch (DataAccessException ex) {
            log.warn("Failed to delete {} stale AI catalog chunk(s)", menuItemIds.size(), ex);
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

    @Override
    public List<AiCatalogVectorHit> searchByKeyword(String queryText, int topK) {
        if (!isReady() || queryText == null || queryText.isBlank()) {
            return List.of();
        }

        try {
            return jdbcTemplate.query(
                    """
                    SELECT menu_item_id,
                           restaurant_id,
                           chunk_text,
                           ts_rank(to_tsvector('simple', chunk_text), plainto_tsquery('simple', ?)) AS rank
                    FROM ai_catalog_chunks
                    WHERE to_tsvector('simple', chunk_text) @@ plainto_tsquery('simple', ?)
                    ORDER BY rank DESC
                    LIMIT ?
                    """,
                    (rs, rowNum) -> new AiCatalogVectorHit(
                            UUID.fromString(rs.getString("menu_item_id")),
                            UUID.fromString(rs.getString("restaurant_id")),
                            rs.getString("chunk_text"),
                            rs.getDouble("rank")
                    ),
                    queryText,
                    queryText,
                    topK
            );
        } catch (DataAccessException ex) {
            log.warn("Keyword (full-text) search against AI catalog chunks failed; continuing with vector results only", ex);
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
