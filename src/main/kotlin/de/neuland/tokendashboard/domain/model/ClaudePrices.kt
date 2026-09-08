package de.neuland.tokendashboard.domain.model

import kotlin.math.roundToLong

data class ClaudePrices(
    val inputCentPerMillion: CentPerMillionToken,
    val outputCentPerMillion: CentPerMillionToken,
    val cacheWrite5mCentPerMillion: CentPerMillionToken,
    val cacheWrite1hCentPerMillion: CentPerMillionToken,
    val cacheReadCentPerMillion: CentPerMillionToken,
) {
    fun calculateCost(tokenCounts: IncomingTokenCounts): DollarCent {
        val cacheWriteCent =
            tokenCounts.cacheWriteTokens.billedAt1hRate() * cacheWrite1hCentPerMillion.centPerToken() +
                tokenCounts.cacheWriteTokens.tokens5m * cacheWrite5mCentPerMillion.centPerToken()
        val cent =
            tokenCounts.inputTokens * inputCentPerMillion.centPerToken() +
                tokenCounts.outputTokens * outputCentPerMillion.centPerToken() +
                cacheWriteCent +
                tokenCounts.cacheReadTokens * cacheReadCentPerMillion.centPerToken()
        return DollarCent(cent.roundToLong())
    }

    private operator fun TokenCount.times(other: Double): Double = value.times(other)
}
