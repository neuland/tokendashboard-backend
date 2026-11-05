package de.neuland.tokendashboard.domain.model

data class ClaudeRawModelUsage(
    val model: ModelName,
    val tokens: ClaudeTokenCounts,
)
