package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Enables pgvector and ANN index for ai_catalog_chunks on PostgreSQL-compatible databases.
 * This migration no-ops for non-PostgreSQL engines (e.g., H2 tests).
 */
public class V15__enable_pgvector_for_ai_rag extends BaseJavaMigration {

    private static final String[] POSTGRES_SQL = new String[] {
        "CREATE EXTENSION IF NOT EXISTS vector",
        "ALTER TABLE ai_catalog_chunks ADD COLUMN IF NOT EXISTS embedding vector(768)",
        "UPDATE ai_catalog_chunks SET embedding = CAST(embedding_text AS vector) "
            + "WHERE embedding IS NULL AND embedding_text IS NOT NULL AND embedding_text <> ''",
        "CREATE INDEX IF NOT EXISTS ai_catalog_chunks_embedding_ivfflat_idx "
            + "ON ai_catalog_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)",
        "ANALYZE ai_catalog_chunks"
    };

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgreSql(connection)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            for (String sql : POSTGRES_SQL) {
                statement.execute(sql);
            }
        }
    }

    private static boolean isPostgreSql(Connection connection) {
        try {
            DatabaseMetaData meta = connection.getMetaData();
            String product = meta == null ? null : meta.getDatabaseProductName();
            return product != null && product.toLowerCase().contains("postgresql");
        } catch (SQLException ex) {
            return false;
        }
    }
}
