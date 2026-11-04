package de.neuland.tokendashboard.application

import arrow.core.NonEmptyList
import de.neuland.tokendashboard.domain.model.IncomingTokenCounts
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptId
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.UserId

data class IngestClaudeUsageRecordCommand(
    val userId: UserId,
    val prompts: NonEmptyList<Prompt>,
    val pluginVersion: PluginVersion,
) {
    companion object {
        private const val SYNTHETIC_MODEL = "<synthetic>"
    }

    fun nonSyntheticPrompts() = prompts.filter { it.model.value != SYNTHETIC_MODEL }

    data class Prompt(
        val model: ModelName,
        val timestamp: PromptTimestamp,
        val sessionId: SessionId,
        val promptId: PromptId,
        val tokenCounts: IncomingTokenCounts,
    )
}
