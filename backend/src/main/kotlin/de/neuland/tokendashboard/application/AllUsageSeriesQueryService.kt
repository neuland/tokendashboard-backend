package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.QueryAllUsageSeriesPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.buildUsageBuckets
import de.neuland.tokendashboard.domain.model.AllProviderSeriesBucket
import de.neuland.tokendashboard.domain.model.AllProviderUsageSeries
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.UsageBucket
import de.neuland.tokendashboard.domain.rollUp

class AllUsageSeriesQueryService(
    private val usageSources: ProviderUsageSources,
    private val userRepository: UserRepositoryPort,
) : QueryAllUsageSeriesPort {
    override fun queryAllUsageSeries(
        range: DayRange,
        granularity: Granularity,
    ): AllProviderUsageSeries {
        val bucketsByProvider = Provider.entries.associateWith { provider -> buckets(provider, range, granularity) }

        return AllProviderUsageSeries(range, AllProviderSeriesBucket.byDate(bucketsByProvider))
    }

    private fun buckets(
        provider: Provider,
        range: DayRange,
        granularity: Granularity,
    ): List<UsageBucket> {
        val dayGroups = usageSources.of(provider).dailyUsage(range).rollUp(granularity)
        val pluginInstallationsByDate = userRepository.pluginInstallationsByDate(range, granularity, provider)
        return buildUsageBuckets(dayGroups, pluginInstallationsByDate)
    }
}
