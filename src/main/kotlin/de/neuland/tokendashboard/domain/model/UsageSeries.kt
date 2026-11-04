package de.neuland.tokendashboard.domain.model

data class UsageBucket(
    val date: Day,
    val modelUsages: ModelUsages,
    val pluginInstallations: UserCount,
) {
    val overallTotal: UsageTotals = modelUsages.totals()

    val modelFamilies: List<ModelFamilyUsage> get() = modelUsages.groupByFamily()
}

data class UsageSeries(
    val range: DayRange,
    val buckets: List<UsageBucket>,
)

class AllProviderSeriesBucket private constructor(
    val date: Day,
    val providerBuckets: Map<Provider, UsageBucket>,
) {
    companion object {
        fun byDate(bucketsByProvider: Map<Provider, List<UsageBucket>>): List<AllProviderSeriesBucket> =
            bucketsByProvider
                .flatMap { (provider, buckets) -> buckets.map { provider to it } }
                .groupBy { (_, bucket) -> bucket.date }
                .map { (date, providerBuckets) -> AllProviderSeriesBucket(date, providerBuckets.toMap()) }
                .sortedByDescending { it.date.value }
    }
}

data class AllProviderUsageSeries(
    val range: DayRange,
    val buckets: List<AllProviderSeriesBucket>,
)
