package de.neuland.tokendashboard.adapter.incoming.rest

import arrow.core.toNonEmptyListOrNull
import de.neuland.tokendashboard.application.IngestOpenCodeUsageRecordCommand
import de.neuland.tokendashboard.domain.model.LlmProviderName
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.NanoCent
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptId
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class OpenCodeUsageReport(
    @SerialName("user_id") val userId: String,
    @SerialName("prompts") val entries: List<OpenCodeUsageEntry>,
    @SerialName("plugin_version") val pluginVersion: String,
) {
    fun toCommand(): IngestOpenCodeUsageRecordCommand {
        require(entries.isNotEmpty()) { "prompts must not be empty" }
        require(entries.size <= MAX_PROMPTS_PER_REPORT) {
            "too many prompts: ${entries.size} > $MAX_PROMPTS_PER_REPORT"
        }
        val prompts =
            entries
                .map { entry ->
                    IngestOpenCodeUsageRecordCommand.Prompt(
                        model = ModelName.from(entry.model),
                        llmProvider = LlmProviderName(entry.provider),
                        timestamp = PromptTimestamp(Instant.parse(entry.timestamp)),
                        sessionId = SessionId(entry.sessionId),
                        promptId = PromptId(entry.entryId),
                        tokenCounts =
                            TokenCounts(
                                inputTokens = TokenCount(entry.usage.inputTokens),
                                outputTokens = TokenCount(entry.usage.outputTokens + entry.usage.reasoningTokens),
                                cacheReadTokens = TokenCount(entry.usage.cacheReadTokens),
                                cacheWriteTokens = TokenCount(entry.usage.cacheWriteTokens),
                            ),
                        cost = NanoCent.fromDollar(entry.cost),
                    )
                }
        val nel = prompts.toNonEmptyListOrNull() ?: throw IllegalArgumentException("prompts must not be empty")
        return IngestOpenCodeUsageRecordCommand(userId = UserId(userId), nel, pluginVersion = PluginVersion(pluginVersion))
    }
}

@Serializable
data class OpenCodeUsageEntry(
    val timestamp: String,
    @SerialName("entry_id") val entryId: String,
    @SerialName("session_id") val sessionId: String,
    val model: String,
    val provider: String,
    val usage: OpenCodeUsageTokens,
    val cost: Double,
)

@Serializable
data class OpenCodeUsageTokens(
    @SerialName("input_tokens") val inputTokens: Long,
    @SerialName("output_tokens") val outputTokens: Long,
    @SerialName("cache_write_tokens") val cacheWriteTokens: Long,
    @SerialName("cache_read_tokens") val cacheReadTokens: Long,
    @SerialName("reasoning_tokens") val reasoningTokens: Long,
)
