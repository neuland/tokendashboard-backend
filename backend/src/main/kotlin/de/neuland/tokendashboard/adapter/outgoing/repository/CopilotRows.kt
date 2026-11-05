package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.CopilotUsageRecord
import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.Instant

data class CopilotUsageRow(
    val model: String,
    val timestamp: Instant,
    @param:ColumnName("session_id") val sessionId: String,
    @param:ColumnName("input_tokens") val inputTokens: Long,
    @param:ColumnName("output_tokens") val outputTokens: Long,
    @param:ColumnName("cache_read_tokens") val cacheReadTokens: Long,
    @param:ColumnName("cache_write_tokens") val cacheWriteTokens: Long,
    @param:ColumnName("nano_aiu") val nanoAiu: Long,
    @param:ColumnName("plugin_version") val pluginVersion: String,
) {
    companion object {
        fun fromDomain(record: CopilotUsageRecord) =
            CopilotUsageRow(
                model = record.model.value,
                timestamp = record.timestamp.value,
                sessionId = record.sessionId.value,
                inputTokens = record.tokens.inputTokens.value,
                outputTokens = record.tokens.outputTokens.value,
                cacheReadTokens = record.tokens.cacheReadTokens.value,
                cacheWriteTokens = record.tokens.cacheWriteTokens.value,
                nanoAiu = record.nanoAiu.value,
                pluginVersion = record.pluginVersion.value,
            )
    }
}

data class AggregatedCopilotBucketRow(
    val date: java.time.LocalDate,
    val model: String,
    @param:ColumnName("input_tokens") val inputTokens: Long,
    @param:ColumnName("output_tokens") val outputTokens: Long,
    @param:ColumnName("cache_read_tokens") val cacheReadTokens: Long,
    @param:ColumnName("cache_write_tokens") val cacheWriteTokens: Long,
    @param:ColumnName("nano_aiu") val nanoAiu: Long,
)
