package de.neuland.tokendashboard.domain.model

data class ProviderUsageOverview(
    val usage: ProviderUsageByModel,
    val activeUsersLast4Weeks: UserCount,
)

data class ProviderUsageByModel(
    val provider: Provider,
    val range: DayRange,
    val modelUsages: ModelUsages,
) {
    fun modelUsagesByFamily(): List<ModelFamilyUsage> = modelUsages.groupByFamily()
}
