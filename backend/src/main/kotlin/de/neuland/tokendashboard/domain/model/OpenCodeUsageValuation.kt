package de.neuland.tokendashboard.domain.model

data class OpenCodeUsageValuation(
    val co2Factors: Co2FactorVersions,
) {
    fun valuate(tokensByDay: Map<Day, List<OpenCodeRawModelUsage>>): List<DatedModelUsages> = tokensByDay.valuateDaily(::valuate)

    fun valuate(
        day: Day,
        usage: OpenCodeRawModelUsage,
    ): ModelUsage =
        ModelUsage(
            model = usage.model,
            tokens = usage.tokens,
            costUsdCent = usage.cost.toCent(),
            estimatedCO2 = co2Factors.co2FactorsFor(usage.model, day).calculateCo2Gram(usage.tokens),
        )
}
