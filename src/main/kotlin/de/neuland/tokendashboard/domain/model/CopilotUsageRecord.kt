package de.neuland.tokendashboard.domain.model

data class CopilotUsageRecord(
    val model: ModelName,
    val timestamp: PromptTimestamp,
    val sessionId: SessionId,
    val tokens: TokenCounts,
    val nanoAiu: NanoAiu,
    val pluginVersion: PluginVersion,
)
