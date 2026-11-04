package de.neuland.tokendashboard.domain.model

data class CopilotUsageValuation(
    val co2Factors: Co2FactorVersions,
) {
    fun valuate(tokensByDay: Map<Day, List<CopilotRawModelUsage>>): List<DatedModelUsages> = tokensByDay.valuateDaily(::valuate)

    fun valuate(
        day: Day,
        usage: CopilotRawModelUsage,
    ): ModelUsage =
        ModelUsage(
            model = usage.model,
            tokens = usage.tokens,
            costUsdCent = DollarCent(usage.nanoAiu.toAiu()),
            estimatedCO2 = co2Factors.co2FactorsFor(usage.model, day).calculateCo2Gram(usage.tokens),
        )
}
