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
        INSERT INTO open_code_usage_records (model, llm_provider, timestamp, session_id, prompt_id, input_tokens, output_tokens, cache_read_tokens, cache_write_tokens, cost_nano_cent, co2_gram, plugin_version)
        VALUES (:model, :llmProvider, :timestamp, :sessionId, :promptId, :inputTokens, :outputTokens, :cacheReadTokens, :cacheWriteTokens, :costNanoCent, :co2Gram, :pluginVersion)
        ON CONFLICT (session_id, prompt_id) DO NOTHING
    """,
    )
    fun insertAll(
        @BindKotlin rows: List<OpenCodeUsageRow>,
    )

    @SqlQuery(
        """
        SELECT model,
            COALESCE(SUM(input_tokens), 0)             AS input_tokens,
            COALESCE(SUM(output_tokens), 0)            AS output_tokens,
            COALESCE(SUM(cache_read_tokens), 0)        AS cache_read_tokens,
            COALESCE(SUM(cache_write_tokens), 0)       AS cache_write_tokens,
            COALESCE(SUM(cost_nano_cent), 0)           AS cost_nano_cent,
            COALESCE(SUM(co2_gram), 0)                 AS co2_gram
        FROM open_code_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
        GROUP BY model
    """,
    )
    @RegisterKotlinMapper(AggregatedOpenCodeRow::class)
    fun aggregateOpenCodeByModel(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): List<AggregatedOpenCodeRow>

    @SqlQuery(
        """
        SELECT
               SUM(input_tokens + output_tokens)    AS tokens,
               SUM(cost_nano_cent)                  AS cost_nano_cent,
               SUM(co2_gram)                        AS co2_gram
        FROM open_code_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
    """,
    )
    @RegisterKotlinMapper(AggregatedOpenCodeTotalsRow::class)
    fun aggregateOpenCodeTotals(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): AggregatedOpenCodeTotalsRow

    @SqlQuery(
        """
        SELECT timestamp::date AS date, model,
            COALESCE(SUM(input_tokens), 0)          AS input_tokens,
            COALESCE(SUM(output_tokens), 0)         AS output_tokens,
            COALESCE(SUM(cache_write_tokens), 0)    AS cache_write_tokens,
            COALESCE(SUM(cache_read_tokens), 0)     AS cache_read_tokens,
            COALESCE(SUM(cost_nano_cent), 0)         AS cost_nano_cent,
            COALESCE(SUM(co2_gram), 0)              AS co2_gram
        FROM open_code_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
        GROUP BY timestamp::date, model
        ORDER BY timestamp::date DESC, model
    """,
    )
    @RegisterKotlinMapper(AggregatedOpenCodeBucketRow::class)
    fun aggregateOpenCodeByDayAndModel(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): List<AggregatedOpenCodeBucketRow>

    @SqlQuery(
        """
        SELECT date_trunc('week', timestamp)::date AS date,
            model,
            COALESCE(SUM(input_tokens), 0)          AS input_tokens,
            COALESCE(SUM(output_tokens), 0)         AS output_tokens,
            COALESCE(SUM(cache_write_tokens), 0)    AS cache_write_tokens,
            COALESCE(SUM(cache_read_tokens), 0)     AS cache_read_tokens,
            COALESCE(SUM(cost_nano_cent), 0)         AS cost_nano_cent,
            COALESCE(SUM(co2_gram), 0)              AS co2_gram
        FROM open_code_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
        GROUP BY date_trunc('week', timestamp), model
        ORDER BY date_trunc('week', timestamp) DESC, model
    """,
    )
    @RegisterKotlinMapper(AggregatedOpenCodeBucketRow::class)
    fun aggregateOpenCodeByWeekAndModel(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): List<AggregatedOpenCodeBucketRow>
}
