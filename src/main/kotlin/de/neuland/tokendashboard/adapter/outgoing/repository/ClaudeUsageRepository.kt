package de.neuland.tokendashboard.adapter.outgoing.repository

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import java.time.LocalDate

interface ClaudeUsageRepository {
    @SqlBatch(
        """
        INSERT INTO claude_usage_records (model, timestamp, session_id, prompt_id, input_tokens, output_tokens, cache_read_tokens, cache_write_tokens, cache_write_5m_tokens, cache_write_1h_tokens, plugin_version)
        VALUES (:model, :timestamp, :sessionId, :promptId, :inputTokens, :outputTokens, :cacheReadTokens, :cacheWriteTokens, :cacheWrite5mTokens, :cacheWrite1hTokens, :pluginVersion)
        ON CONFLICT (session_id, prompt_id) DO UPDATE SET
            input_tokens = GREATEST(claude_usage_records.input_tokens, EXCLUDED.input_tokens),
            output_tokens = GREATEST(claude_usage_records.output_tokens, EXCLUDED.output_tokens),
            cache_read_tokens = GREATEST(claude_usage_records.cache_read_tokens, EXCLUDED.cache_read_tokens),
            cache_write_tokens = GREATEST(claude_usage_records.cache_write_tokens, EXCLUDED.cache_write_tokens),
            cache_write_5m_tokens = GREATEST(claude_usage_records.cache_write_5m_tokens, EXCLUDED.cache_write_5m_tokens),
            cache_write_1h_tokens = GREATEST(claude_usage_records.cache_write_1h_tokens, EXCLUDED.cache_write_1h_tokens)
    """,
    )
    fun insertAll(
        @BindKotlin rows: List<ClaudeUsageRow>,
    )

    @SqlQuery(
        """
        SELECT (timestamp AT TIME ZONE <reportingTimeZone>)::date AS date, model,
            COALESCE(SUM(input_tokens), 0)          AS input_tokens,
            COALESCE(SUM(output_tokens), 0)         AS output_tokens,
            COALESCE(SUM(cache_write_5m_tokens), 0) AS cache_write_5m_tokens,
            COALESCE(SUM(cache_write_1h_tokens), 0) AS cache_write_1h_tokens,
            COALESCE(SUM(cache_write_tokens), 0)    AS cache_write_tokens,
            COALESCE(SUM(cache_read_tokens), 0)     AS cache_read_tokens
        FROM claude_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
        GROUP BY (timestamp AT TIME ZONE <reportingTimeZone>)::date, model
        ORDER BY (timestamp AT TIME ZONE <reportingTimeZone>)::date DESC, model
    """,
    )
    @RegisterKotlinMapper(AggregatedClaudeBucketRow::class)
    fun aggregateClaudeByDayAndModel(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): List<AggregatedClaudeBucketRow>
}
