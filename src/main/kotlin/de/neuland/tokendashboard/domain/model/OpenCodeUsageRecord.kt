package de.neuland.tokendashboard.domain.model

data class OpenCodeUsageRecord(
    val model: ModelName,
    val llmProvider: LlmProviderName,
    val timestamp: PromptTimestamp,
    val sessionId: SessionId,
    val promptId: PromptId,
    val tokens: TokenCounts,
    val costNanoCent: NanoCent,
    val co2Gram: Co2Gram,
    val pluginVersion: PluginVersion,
)
