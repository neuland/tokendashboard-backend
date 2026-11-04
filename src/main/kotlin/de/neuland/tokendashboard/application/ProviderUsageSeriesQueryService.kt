package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.QueryProviderUsageSeriesPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.buildUsageBuckets
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.UsageSeries
import de.neuland.tokendashboard.domain.rollUp

class ProviderUsageSeriesQueryService(
    private val usageSources: ProviderUsageSources,
    private val userRepository: UserRepositoryPort,
) : QueryProviderUsageSeriesPort {
    override fun querySeries(
        provider: Provider,
        range: DayRange,
        granularity: Granularity,
    ): UsageSeries {
        val dayGroups = usageSources.of(provider).dailyUsage(range).rollUp(granularity)
        val pluginInstallationsByDate = userRepository.pluginInstallationsByDate(range, granularity, provider)

        return UsageSeries(range, buildUsageBuckets(dayGroups, pluginInstallationsByDate))
    }
}
