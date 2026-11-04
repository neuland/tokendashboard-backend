package de.neuland.tokendashboard.domain.model

data class ClaudeUsageValuation(
    val prices: ClaudePriceVersions,
    val co2Factors: Co2FactorVersions,
) {
    fun valuate(tokensByDay: Map<Day, List<ClaudeRawModelUsage>>): List<DatedModelUsages> = tokensByDay.valuateDaily(::valuate)

    fun valuate(
        day: Day,
        usage: ClaudeRawModelUsage,
    ): ModelUsage {
        val tokenCounts = usage.tokens.toTokenCounts()
        return ModelUsage(
            model = usage.model,
            tokens = tokenCounts,
            costUsdCent = prices.pricesFor(usage.model, day).calculateCost(usage.tokens),
            estimatedCO2 = co2Factors.co2FactorsFor(usage.model, day).calculateCo2Gram(tokenCounts),
        )
    }
}
