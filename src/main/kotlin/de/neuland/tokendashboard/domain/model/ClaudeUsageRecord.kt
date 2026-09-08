package de.neuland.tokendashboard.domain.model

data class ClaudeUsageRecord(
    val model: ModelName,
    val timestamp: PromptTimestamp,
    val sessionId: SessionId,
    val promptId: PromptId,
    val tokens: IncomingTokenCounts,
    val costUsdCent: DollarCent,
    val co2Gram: Co2Gram,
    val pluginVersion: PluginVersion,
)
