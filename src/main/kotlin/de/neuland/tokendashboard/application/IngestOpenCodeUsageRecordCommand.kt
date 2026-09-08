package de.neuland.tokendashboard.application

import arrow.core.NonEmptyList
import de.neuland.tokendashboard.domain.model.LlmProviderName
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.NanoCent
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptId
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UserId

data class IngestOpenCodeUsageRecordCommand(
    val userId: UserId,
    val prompts: NonEmptyList<Prompt>,
    val pluginVersion: PluginVersion,
) {
    fun nonZeroPrompts() = prompts.filter { it.containsTokens() }

    data class Prompt(
        val model: ModelName,
        val llmProvider: LlmProviderName,
        val timestamp: PromptTimestamp,
        val sessionId: SessionId,
        val promptId: PromptId,
        val tokenCounts: TokenCounts,
        val cost: NanoCent,
    ) {
        fun containsTokens(): Boolean = tokenCounts.sumOverAll().value > 0
    }
}
