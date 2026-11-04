package de.neuland.tokendashboard.application

import arrow.core.NonEmptyList
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.NanoAiu
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UserId

data class IngestCopilotUsageRecordCommand(
    val userId: UserId,
    val prompts: NonEmptyList<Prompt>,
    val pluginVersion: PluginVersion,
) {
    data class Prompt(
        val model: ModelName,
        val timestamp: PromptTimestamp,
        val sessionId: SessionId,
        val tokenCounts: TokenCounts,
        val nanoAiu: NanoAiu,
    )
}
