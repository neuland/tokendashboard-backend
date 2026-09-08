package de.neuland.tokendashboard.adapter.incoming.rest

import arrow.core.toNonEmptyListOrNull
import de.neuland.tokendashboard.application.IngestCopilotUsageRecordCommand
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.NanoAiu
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class CopilotUsageReport(
    @SerialName("user_id") val userId: String,
    @SerialName("prompts") val entries: List<CopilotUsageEntry>,
    @SerialName("plugin_version") val pluginVersion: String,
) {
    fun toCommand(): IngestCopilotUsageRecordCommand {
        require(entries.size <= MAX_PROMPTS_PER_REPORT) {
            "too many prompts: ${entries.size} > $MAX_PROMPTS_PER_REPORT"
        }
        val prompts =
            entries.map { entry ->
                IngestCopilotUsageRecordCommand.Prompt(
                    model = ModelName.from(entry.model),
                    timestamp = PromptTimestamp(Instant.parse(entry.timestamp)),
                    sessionId = SessionId(entry.sessionId),
                    tokenCounts =
                        TokenCounts(
                            inputTokens = TokenCount(entry.usage.inputTokens),
                            outputTokens = TokenCount(entry.usage.outputTokens),
                            cacheReadTokens = TokenCount(entry.usage.cacheReadTokens),
                            cacheWriteTokens = TokenCount(entry.usage.cacheWriteTokens),
                        ),
                    nanoAiu = NanoAiu(entry.nanoAiu),
                )
            }
        val nel = prompts.toNonEmptyListOrNull() ?: throw IllegalArgumentException("prompts must not be empty")
        return IngestCopilotUsageRecordCommand(userId = UserId(userId), prompts = nel, pluginVersion = PluginVersion(pluginVersion))
    }
}

@Serializable
data class CopilotUsageEntry(
    val timestamp: String,
    @SerialName("session_id") val sessionId: String,
    val model: String,
    val usage: CopilotUsageTokens,
    val requests: Int = 0,
    @SerialName("total_nano_aiu") val nanoAiu: Long,
)

@Serializable
data class CopilotUsageTokens(
    @SerialName("input_tokens") val inputTokens: Long,
    @SerialName("output_tokens") val outputTokens: Long,
    @SerialName("cache_read_tokens") val cacheReadTokens: Long,
    @SerialName("cache_write_tokens") val cacheWriteTokens: Long,
    // Copilot already includes reasoning tokens in its reported output_tokens. Is deliberately discarded.
    @SerialName("reasoning_tokens") val reasoningTokens: Long,
)
