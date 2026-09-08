package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.OpenCodeUsageRecord
import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.Instant
import java.time.LocalDate

data class AggregatedOpenCodeRow(
    val model: String,
    @param:ColumnName("input_tokens") val inputTokens: Long,
    @param:ColumnName("output_tokens") val outputTokens: Long,
    @param:ColumnName("cache_write_tokens") val cacheWriteTokens: Long,
    @param:ColumnName("cache_read_tokens") val cacheReadTokens: Long,
    @param:ColumnName("cost_nano_cent") val costNanoCent: Long,
    @param:ColumnName("co2_gram") val co2Gram: Double,
)

data class OpenCodeUsageRow(
    val model: String,
    @param:ColumnName("llm_provider") val llmProvider: String,
    val timestamp: Instant,
    @param:ColumnName("session_id") val sessionId: String,
    @param:ColumnName("prompt_id") val promptId: String,
    @param:ColumnName("input_tokens") val inputTokens: Long,
    @param:ColumnName("output_tokens") val outputTokens: Long,
    @param:ColumnName("cache_write_tokens") val cacheWriteTokens: Long,
    @param:ColumnName("cache_read_tokens") val cacheReadTokens: Long,
    @param:ColumnName("cost_nano_cent") val costNanoCent: Long,
    @param:ColumnName("co2_gram") val co2Gram: Double,
    @param:ColumnName("plugin_version") val pluginVersion: String,
) {
    companion object {
        fun fromDomain(record: OpenCodeUsageRecord) =
            OpenCodeUsageRow(
                model = record.model.value,
                llmProvider = record.llmProvider.value,
                timestamp = record.timestamp.value,
                sessionId = record.sessionId.value,
                promptId = record.promptId.value,
                inputTokens = record.tokens.inputTokens.value,
                outputTokens = record.tokens.outputTokens.value,
                cacheWriteTokens = record.tokens.cacheWriteTokens.value,
                cacheReadTokens = record.tokens.cacheReadTokens.value,
                costNanoCent = record.costNanoCent.value,
                co2Gram = record.co2Gram.value,
                pluginVersion = record.pluginVersion.value,
            )
    }
}

data class AggregatedOpenCodeTotalsRow(
    @param:ColumnName("tokens") val tokens: Long,
    @param:ColumnName("cost_nano_cent") val costNanoCent: Long,
    @param:ColumnName("co2_gram") val co2Gram: Double,
)

data class AggregatedOpenCodeBucketRow(
    val date: LocalDate,
    val model: String,
    @param:ColumnName("input_tokens") val inputTokens: Long,
    @param:ColumnName("output_tokens") val outputTokens: Long,
    @param:ColumnName("cache_write_tokens") val cacheWriteTokens: Long,
    @param:ColumnName("cache_read_tokens") val cacheReadTokens: Long,
    @param:ColumnName("cost_nano_cent") val costNanoCent: Long,
    @param:ColumnName("co2_gram") val co2Gram: Double,
)
