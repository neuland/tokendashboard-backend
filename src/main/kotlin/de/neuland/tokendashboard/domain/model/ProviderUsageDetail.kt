package de.neuland.tokendashboard.domain.model

import arrow.core.NonEmptyList

data class ProviderPluginUsage(
    val usage: ProviderUsageDetail,
    val activeUsersLast4Weeks: UserCount,
)

data class ProviderUsageDetail(
    val provider: Provider,
    val range: DayRange,
    val modelUsages: UsageResult,
) {
    fun modelUsagesByFamily(): List<ModelFamilyUsage> = modelUsages.groupByFamily()
}

data class ModelFamilyUsage(
    val modelFamily: ModelFamily,
    val modelUsages: NonEmptyList<ModelUsage>,
)

data class ModelUsage(
    val model: ModelName,
    val tokens: TokenCounts,
    val costUsdCent: DollarCent,
    val estimatedCO2: Co2Gram,
)

data class ModelUsagesAtDate(
    val date: Day,
    val modelUsages: UsageResult,
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

    companion object {
        fun zero() = UsageTotals(TokenCounts.zero(), DollarCent(0), Co2Gram(0.0))
    }
}

data class UsageBucket(
    val date: Day,
    val modelUsages: UsageResult,
    val pluginInstallations: UserCount,
) {
    val overallTotal: UsageTotals =
        when (modelUsages) {
            is UsageResult.NoUsage -> {
                UsageTotals.zero()
            }

            is UsageResult.HasUsage -> {
                modelUsages.entries.fold(UsageTotals.zero()) { acc, usage -> acc + usage }
            }
        }

    val modelFamilies: List<ModelFamilyUsage> get() = modelUsages.groupByFamily()
}

data class UsageSeries(
    val range: DayRange,
    val buckets: List<UsageBucket>,
)

data class AllProviderSeriesBucket(
    val date: Day,
    val providerBuckets: Map<Provider, UsageBucket>,
)

data class AllProviderUsageSeries(
    val range: DayRange,
    val buckets: List<AllProviderSeriesBucket>,
)
