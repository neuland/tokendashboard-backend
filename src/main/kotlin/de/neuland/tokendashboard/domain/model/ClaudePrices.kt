package de.neuland.tokendashboard.domain.model

import kotlin.math.roundToLong

data class ClaudePrices(
    val inputCentPerMillion: CentPerMillionToken,
    val outputCentPerMillion: CentPerMillionToken,
    val cacheWrite5mCentPerMillion: CentPerMillionToken,
    val cacheWrite1hCentPerMillion: CentPerMillionToken,
    val cacheReadCentPerMillion: CentPerMillionToken,
) {
    companion object {
        val ZERO =
            ClaudePrices(
                CentPerMillionToken.ZERO,
                CentPerMillionToken.ZERO,
                CentPerMillionToken.ZERO,
                CentPerMillionToken.ZERO,
                CentPerMillionToken.ZERO,
            )
    }

    fun calculateCost(tokenCounts: ClaudeTokenCounts): DollarCent {
        val cacheWriteCent =
            tokenCounts.cacheWriteBilledAt1hRate() * cacheWrite1hCentPerMillion.centPerToken() +
                tokenCounts.cacheWriteTokens5m * cacheWrite5mCentPerMillion.centPerToken()
        val cent =
            tokenCounts.inputTokens * inputCentPerMillion.centPerToken() +
                tokenCounts.outputTokens * outputCentPerMillion.centPerToken() +
                cacheWriteCent +
                tokenCounts.cacheReadTokens * cacheReadCentPerMillion.centPerToken()
        return DollarCent(cent.roundToLong())
    }

    private operator fun TokenCount.times(other: Double): Double = value.times(other)
}
