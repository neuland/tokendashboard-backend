package de.neuland.tokendashboard.domain.model

data class CopilotRawModelUsage(
    val model: ModelName,
    val tokens: TokenCounts,
    val nanoAiu: NanoAiu,
)
