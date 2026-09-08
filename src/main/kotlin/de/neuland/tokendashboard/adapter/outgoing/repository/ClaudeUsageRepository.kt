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
        INSERT INTO claude_usage_records (model, timestamp, session_id, prompt_id, input_tokens, output_tokens, cache_read_tokens, cache_write_tokens, cache_write_5m_tokens, cache_write_1h_tokens, cost_usd_cent, co2_gram, plugin_version)
        VALUES (:model, :timestamp, :sessionId, :promptId, :inputTokens, :outputTokens, :cacheReadTokens, :cacheWriteTokens, :cacheWrite5mTokens, :cacheWrite1hTokens, :costUsdCent, :co2Gram, :pluginVersion)
        ON CONFLICT (session_id, prompt_id) DO NOTHING
    """,
    )
    fun insertAll(
        @BindKotlin rows: List<ClaudeUsageRow>,
    )

    @SqlQuery(
        """
        SELECT model,
            COALESCE(SUM(input_tokens), 0)             AS input_tokens,
            COALESCE(SUM(output_tokens), 0)            AS output_tokens,
            COALESCE(SUM(cache_write_5m_tokens), 0)    AS cache_write_5m_tokens,
            COALESCE(SUM(cache_write_1h_tokens), 0)    AS cache_write_1h_tokens,
            COALESCE(SUM(cache_write_tokens), 0)       AS cache_write_tokens,
            COALESCE(SUM(cache_read_tokens), 0)        AS cache_read_tokens,
            COALESCE(SUM(cost_usd_cent), 0)            AS cost_usd_cent,
            COALESCE(SUM(co2_gram), 0)                 AS co2_gram
        FROM claude_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
        GROUP BY model
    """,
    )
    @RegisterKotlinMapper(AggregatedClaudeRow::class)
    fun aggregateClaudeByModel(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): List<AggregatedClaudeRow>

    @SqlQuery(
        """
        SELECT
               SUM(input_tokens + output_tokens)    AS tokens,
               SUM(cost_usd_cent)                   AS cost_usd_cent,
               SUM(co2_gram)                        AS co2_gram
        FROM claude_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
    """,
    )
    @RegisterKotlinMapper(AggregatedClaudeTotalsRow::class)
    fun aggregateClaudeTotals(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): AggregatedClaudeTotalsRow

    @SqlQuery(
        """
        SELECT timestamp::date AS date, model,
            COALESCE(SUM(input_tokens), 0)          AS input_tokens,
            COALESCE(SUM(output_tokens), 0)         AS output_tokens,
            COALESCE(SUM(cache_write_5m_tokens), 0) AS cache_write_5m_tokens,
            COALESCE(SUM(cache_write_1h_tokens), 0) AS cache_write_1h_tokens,
            COALESCE(SUM(cache_write_tokens), 0)    AS cache_write_tokens,
            COALESCE(SUM(cache_read_tokens), 0)     AS cache_read_tokens,
            COALESCE(SUM(cost_usd_cent), 0)         AS cost_usd_cent,
            COALESCE(SUM(co2_gram), 0)              AS co2_gram
        FROM claude_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
        GROUP BY timestamp::date, model
        ORDER BY timestamp::date DESC, model
    """,
    )
    @RegisterKotlinMapper(AggregatedClaudeBucketRow::class)
    fun aggregateClaudeByDayAndModel(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): List<AggregatedClaudeBucketRow>

    @SqlQuery(
        """
        SELECT date_trunc('week', timestamp)::date AS date,
            model,
            COALESCE(SUM(input_tokens), 0)          AS input_tokens,
            COALESCE(SUM(output_tokens), 0)         AS output_tokens,
            COALESCE(SUM(cache_write_5m_tokens), 0) AS cache_write_5m_tokens,
            COALESCE(SUM(cache_write_1h_tokens), 0) AS cache_write_1h_tokens,
            COALESCE(SUM(cache_write_tokens), 0)    AS cache_write_tokens,
            COALESCE(SUM(cache_read_tokens), 0)     AS cache_read_tokens,
            COALESCE(SUM(cost_usd_cent), 0)         AS cost_usd_cent,
            COALESCE(SUM(co2_gram), 0)              AS co2_gram
        FROM claude_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
        GROUP BY date_trunc('week', timestamp), model
        ORDER BY date_trunc('week', timestamp) DESC, model
    """,
    )
    @RegisterKotlinMapper(AggregatedClaudeBucketRow::class)
    fun aggregateClaudeByWeekAndModel(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): List<AggregatedClaudeBucketRow>
}
