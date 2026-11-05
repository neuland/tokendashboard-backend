package de.neuland.tokendashboard.domain.model

import arrow.core.NonEmptyList

data class ModelUsage(
    val model: ModelName,
    val tokens: TokenCounts,
    val costUsdCent: DollarCent,
    val estimatedCO2: Co2Gram,
)

data class ModelFamilyUsage(
    val modelFamily: ModelFamily,
    val modelUsages: NonEmptyList<ModelUsage>,
)

data class DatedModelUsages(
    val date: Day,
    val modelUsages: ModelUsages,
)

data class UsageTotals(
    val tokens: TokenCounts,
    val costUsdCent: DollarCent,
    val estimatedCO2: Co2Gram,
) {
    operator fun plus(usage: ModelUsage) =
        UsageTotals(
            tokens = tokens + usage.tokens,
            costUsdCent = costUsdCent + usage.costUsdCent,
            estimatedCO2 = estimatedCO2 + usage.estimatedCO2,
        )

    fun tokensInOut(): TokenCount = tokens.inputTokens + tokens.outputTokens

    companion object {
        fun zero() = UsageTotals(TokenCounts.zero(), DollarCent(0), Co2Gram(0.0))
    }
}
