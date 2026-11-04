package de.neuland.tokendashboard.domain.model

data class OpenCodeRawModelUsage(
    val model: ModelName,
    val tokens: TokenCounts,
    val cost: NanoCent,
)
