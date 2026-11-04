package de.neuland.tokendashboard.adapter.incoming.rest

import arrow.core.toNonEmptyListOrNull
import de.neuland.tokendashboard.application.IngestClaudeUsageRecordCommand
import de.neuland.tokendashboard.domain.model.IncomingCacheWriteTokens
import de.neuland.tokendashboard.domain.model.IncomingTokenCounts
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptId
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ClaudeUsageReport(
    @SerialName("user_id") val userId: String,
    @SerialName("prompts") val entries: List<ClaudeUsageEntry>,
    @SerialName("plugin_version") val pluginVersion: String,
) {
    fun toCommand(): IngestClaudeUsageRecordCommand {
        require(entries.size <= MAX_PROMPTS_PER_REPORT) {
            "too many prompts: ${entries.size} > $MAX_PROMPTS_PER_REPORT"
        }
        val prompts =
            this.entries.map { entry ->
                IngestClaudeUsageRecordCommand.Prompt(
                    model = ModelName.from(entry.model),
                    timestamp = PromptTimestamp(Instant.parse(entry.timestamp)),
                    sessionId = SessionId(entry.sessionId),
                    promptId = PromptId(entry.entryId),
                    tokenCounts =
                        IncomingTokenCounts(
                            inputTokens = TokenCount(entry.usage.inputTokens),
                            outputTokens = TokenCount(entry.usage.outputTokens),
                            cacheReadTokens = TokenCount(entry.usage.cacheReadInputTokens),
                            cacheWriteTokens = entry.usage.toIncomingCacheWriteTokens(),
                        ),
                )
            }
        val nel = prompts.toNonEmptyListOrNull() ?: throw IllegalArgumentException("prompts must not be empty")
        return IngestClaudeUsageRecordCommand(
            userId = UserId(userId),
            prompts = nel,
            pluginVersion = PluginVersion(pluginVersion),
        )
    }
}

@Serializable
data class ClaudeUsageEntry(
    val timestamp: String,
    @SerialName("entry_id") val entryId: String,
    @SerialName("session_id") val sessionId: String,
    val model: String,
    val usage: ClaudeUsageTokens,
)

@Serializable
data class ClaudeUsageTokens(
    @SerialName("input_tokens") val inputTokens: Long,
    @SerialName("output_tokens") val outputTokens: Long,
    @SerialName("ephemeral_5m_input_tokens") val cacheCreationInputTokens5m: Long = 0,
    @SerialName("ephemeral_1h_input_tokens") val cacheCreationInputTokens1h: Long = 0,
    @SerialName("cache_creation_input_tokens") val cacheCreationInputTokens: Long = 0,
    @SerialName("cache_read_input_tokens") val cacheReadInputTokens: Long,
) {
    fun toIncomingCacheWriteTokens(): IncomingCacheWriteTokens =
        IncomingCacheWriteTokens.fromReportedTotals(
            reportedTotal = TokenCount(cacheCreationInputTokens),
            tokens5m = TokenCount(cacheCreationInputTokens5m),
            tokens1h = TokenCount(cacheCreationInputTokens1h),
        )
}
