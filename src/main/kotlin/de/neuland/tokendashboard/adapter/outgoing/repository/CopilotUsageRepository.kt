package de.neuland.tokendashboard.adapter.outgoing.repository

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import java.time.LocalDate

interface CopilotUsageRepository {
    @SqlBatch(
        """
        INSERT INTO copilot_usage_records (model, timestamp, session_id, input_tokens, output_tokens, cache_read_tokens, cache_write_tokens, nano_aiu, co2_gram, plugin_version)
        VALUES (:model, :timestamp, :sessionId, :inputTokens, :outputTokens, :cacheReadTokens, :cacheWriteTokens, :nanoAiu, :co2Gram, :pluginVersion)
        ON CONFLICT (session_id) DO NOTHING
        """,
    )
    fun insertAll(
        @BindKotlin rows: List<CopilotUsageRow>,
    )

    @SqlQuery(
        """
        SELECT model,
            COALESCE(SUM(input_tokens), 0)             AS input_tokens,
            COALESCE(SUM(output_tokens), 0)            AS output_tokens,
            COALESCE(SUM(cache_read_tokens), 0)        AS cache_read_tokens,
            COALESCE(SUM(cache_write_tokens), 0)       AS cache_write_tokens,
            COALESCE(SUM(nano_aiu), 0)                 AS nano_aiu,
            COALESCE(SUM(co2_gram), 0)                 AS co2_gram
        FROM copilot_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
        GROUP BY model
        """,
    )
    @RegisterKotlinMapper(AggregatedCopilotRow::class)
    fun aggregateCopilotByModel(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): List<AggregatedCopilotRow>

    @SqlQuery(
        """
        SELECT 
               SUM(input_tokens + output_tokens)    AS tokens,
               SUM(nano_aiu)                        AS nano_aiu,
               SUM(co2_gram)                        AS co2_gram
        FROM copilot_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
    """,
    )
    @RegisterKotlinMapper(AggregatedCopilotTotalsRow::class)
    fun aggregateCopilotTotals(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): AggregatedCopilotTotalsRow

    @SqlQuery(
        """
        SELECT timestamp::date AS date, model,
            COALESCE(SUM(input_tokens), 0)       AS input_tokens,
            COALESCE(SUM(output_tokens), 0)      AS output_tokens,
            COALESCE(SUM(cache_read_tokens), 0)  AS cache_read_tokens,
            COALESCE(SUM(cache_write_tokens), 0) AS cache_write_tokens,
            COALESCE(SUM(nano_aiu), 0)           AS nano_aiu,
            COALESCE(SUM(co2_gram), 0)           AS co2_gram
        FROM copilot_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
        GROUP BY timestamp::date, model
        ORDER BY timestamp::date DESC, model
    """,
    )
    @RegisterKotlinMapper(AggregatedCopilotBucketRow::class)
    fun aggregateCopilotByDayAndModel(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): List<AggregatedCopilotBucketRow>

    @SqlQuery(
        """
        SELECT date_trunc('week', timestamp)::date AS date,
            model,
            COALESCE(SUM(input_tokens), 0)       AS input_tokens,
            COALESCE(SUM(output_tokens), 0)      AS output_tokens,
            COALESCE(SUM(cache_read_tokens), 0)  AS cache_read_tokens,
            COALESCE(SUM(cache_write_tokens), 0) AS cache_write_tokens,
            COALESCE(SUM(nano_aiu), 0)           AS nano_aiu,
            COALESCE(SUM(co2_gram), 0)           AS co2_gram
        FROM copilot_usage_records
        WHERE timestamp >= :from AND timestamp < :to + INTERVAL '1 day'
        GROUP BY date_trunc('week', timestamp), model
        ORDER BY date_trunc('week', timestamp) DESC, model
    """,
    )
    @RegisterKotlinMapper(AggregatedCopilotBucketRow::class)
    fun aggregateCopilotByWeekAndModel(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
    ): List<AggregatedCopilotBucketRow>
}
