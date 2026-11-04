package de.neuland.tokendashboard.adapter.outgoing.repository

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import java.time.LocalDate

interface OpenCodeUsageRepository {
    @SqlBatch(
        """
        INSERT INTO open_code_usage_records (model, llm_provider, timestamp, session_id, prompt_id, input_tokens, output_tokens, cache_read_tokens, cache_write_tokens, cost_nano_cent, plugin_version)
        VALUES (:model, :llmProvider, :timestamp, :sessionId, :promptId, :inputTokens, :outputTokens, :cacheReadTokens, :cacheWriteTokens, :costNanoCent, :pluginVersion)
        ON CONFLICT (session_id, prompt_id) DO NOTHING
    """,
    )
    fun insertAll(
        @BindKotlin rows: List<OpenCodeUsageRow>,
    )

    @SqlQuery(
        """
        SELECT (timestamp AT TIME ZONE <reportingTimeZone>)::date AS date, model,
            COALESCE(SUM(input_tokens), 0)          AS input_tokens,
            COALESCE(SUM(output_tokens), 0)         AS output_tokens,
            COALESCE(SUM(cache_write_tokens), 0)    AS cache_write_tokens,
            COALESCE(SUM(cache_read_tokens), 0)     AS cache_read_tokens,
            COALESCE(SUM(cost_nano_cent), 0)         AS cost_nano_cent
        FROM open_code_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
        GROUP BY (timestamp AT TIME ZONE <reportingTimeZone>)::date, model
        ORDER BY (timestamp AT TIME ZONE <reportingTimeZone>)::date DESC, model
    """,
    )
    @RegisterKotlinMapper(AggregatedOpenCodeBucketRow::class)
    fun aggregateOpenCodeByDayAndModel(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): List<AggregatedOpenCodeBucketRow>
}
