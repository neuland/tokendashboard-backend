package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.ClaudeUsageRecord
import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.Instant
import java.time.LocalDate

data class AggregatedClaudeRow(
    val model: String,
    @param:ColumnName("input_tokens") val inputTokens: Long,
    @param:ColumnName("output_tokens") val outputTokens: Long,
    @param:ColumnName("cache_write_5m_tokens") val cacheWrite5mTokens: Long,
    @param:ColumnName("cache_write_1h_tokens") val cacheWrite1hTokens: Long,
    @param:ColumnName("cache_write_tokens") val cacheWriteTokens: Long,
    @param:ColumnName("cache_read_tokens") val cacheReadTokens: Long,
    @param:ColumnName("cost_usd_cent") val costUsdCent: Long,
    @param:ColumnName("co2_gram") val co2Gram: Double,
)

data class ClaudeUsageRow(
    val model: String,
    val timestamp: Instant,
    @param:ColumnName("session_id") val sessionId: String,
    @param:ColumnName("prompt_id") val promptId: String,
    @param:ColumnName("input_tokens") val inputTokens: Long,
    @param:ColumnName("output_tokens") val outputTokens: Long,
    @param:ColumnName("cache_write_5m_tokens") val cacheWrite5mTokens: Long,
    @param:ColumnName("cache_write_1h_tokens") val cacheWrite1hTokens: Long,
    @param:ColumnName("cache_write_tokens") val cacheWriteTokens: Long,
    @param:ColumnName("cache_read_tokens") val cacheReadTokens: Long,
    @param:ColumnName("cost_usd_cent") val costUsdCent: Long,
    @param:ColumnName("co2_gram") val co2Gram: Double,
    @param:ColumnName("plugin_version") val pluginVersion: String,
) {
    companion object {
        fun fromDomain(record: ClaudeUsageRecord): ClaudeUsageRow =
            ClaudeUsageRow(
                model = record.model.value,
                timestamp = record.timestamp.value,
                sessionId = record.sessionId.value,
                promptId = record.promptId.value,
                inputTokens = record.tokens.inputTokens.value,
                outputTokens = record.tokens.outputTokens.value,
                cacheWrite5mTokens = record.tokens.cacheWriteTokens.tokens5m.value,
                cacheWrite1hTokens = record.tokens.cacheWriteTokens.tokens1h.value,
                cacheWriteTokens = record.tokens.cacheWriteTokens.generic.value,
                cacheReadTokens = record.tokens.cacheReadTokens.value,
                costUsdCent = record.costUsdCent.value,
                co2Gram = record.co2Gram.value,
                pluginVersion = record.pluginVersion.value,
            )
    }
}

data class AggregatedClaudeTotalsRow(
    @param:ColumnName("tokens") val tokens: Long,
    @param:ColumnName("cost_usd_cent") val costUsdCent: Long,
    @param:ColumnName("co2_gram") val co2Gram: Double,
)

data class AggregatedClaudeBucketRow(
    val date: LocalDate,
    val model: String,
    @param:ColumnName("input_tokens") val inputTokens: Long,
    @param:ColumnName("output_tokens") val outputTokens: Long,
    @param:ColumnName("cache_write_5m_tokens") val cacheWrite5mTokens: Long,
    @param:ColumnName("cache_write_1h_tokens") val cacheWrite1hTokens: Long,
    @param:ColumnName("cache_write_tokens") val cacheWriteTokens: Long,
    @param:ColumnName("cache_read_tokens") val cacheReadTokens: Long,
    @param:ColumnName("cost_usd_cent") val costUsdCent: Long,
    @param:ColumnName("co2_gram") val co2Gram: Double,
)
