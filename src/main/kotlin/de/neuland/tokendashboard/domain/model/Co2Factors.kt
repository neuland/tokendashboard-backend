package de.neuland.tokendashboard.domain.model

data class Co2Factors(
    val inputFactor: Co2Factor,
    val outputFactor: Co2Factor,
    val cacheReadFactor: Co2Factor,
    val cacheWriteFactor: Co2Factor,
) {
    fun calculateCo2Gram(tokenCounts: TokenCounts): Co2Gram {
        val gram =
            tokenCounts.inputTokens * inputFactor +
                tokenCounts.cacheReadTokens * cacheReadFactor +
                tokenCounts.outputTokens * outputFactor +
                tokenCounts.cacheWriteTokens * cacheWriteFactor
        return Co2Gram(gram)
    }

    fun calculateCo2Gram(tokenCounts: IncomingTokenCounts): Co2Gram {
        val gram =
            tokenCounts.inputTokens * inputFactor +
                tokenCounts.cacheReadTokens * cacheReadFactor +
                tokenCounts.outputTokens * outputFactor +
                tokenCounts.cacheWriteTokens.totalCached() * cacheWriteFactor
        return Co2Gram(gram)
    }

    private operator fun TokenCount.times(factor: Co2Factor): Double = value * factor.gramCo2PerMillionToken / 1_000_000
}
