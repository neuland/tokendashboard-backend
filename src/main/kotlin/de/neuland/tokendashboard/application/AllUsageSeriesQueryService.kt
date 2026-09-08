package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.QueryAllUsageSeriesPort
import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.buildUsageBuckets
import de.neuland.tokendashboard.domain.model.AllProviderSeriesBucket
import de.neuland.tokendashboard.domain.model.AllProviderUsageSeries
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.ModelUsagesAtDate
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.UsageBucket

class AllUsageSeriesQueryService(
    private val claudeRepository: ClaudeUsageRepositoryPort,
    private val copilotRepository: CopilotUsageRepositoryPort,
    private val openCodeRepository: OpenCodeUsageRepositoryPort,
    private val userRepository: UserRepositoryPort,
) : QueryAllUsageSeriesPort {
    override fun queryAllUsageSeries(
        range: DayRange,
        granularity: Granularity,
    ): AllProviderUsageSeries {
        val bucketsByProvider =
            Provider.entries.associateWith { provider ->
                when (provider) {
                    CLAUDE -> buckets(provider, range, granularity, claudeRepository::claudeUsageSeries)
                    COPILOT -> buckets(provider, range, granularity, copilotRepository::copilotUsageSeries)
                    OPENCODE -> buckets(provider, range, granularity, openCodeRepository::openCodeUsageSeries)
                }
            }

        val buckets =
            bucketsByProvider.values
                .flatMap { it.keys }
                .distinct()
                .sortedByDescending { it.value }
                .map { date ->
                    AllProviderSeriesBucket(
                        date = date,
                        providerBuckets =
                            bucketsByProvider
                                .mapNotNull { (provider, bucketsByDay) -> bucketsByDay[date]?.let { provider to it } }
                                .toMap(),
                    )
                }

        return AllProviderUsageSeries(range, buckets)
    }

    private fun buckets(
        provider: Provider,
        range: DayRange,
        granularity: Granularity,
        fetchSeries: (DayRange, Granularity) -> List<ModelUsagesAtDate>,
    ): Map<Day, UsageBucket> {
        val dayGroups = fetchSeries(range, granularity)
        val pluginInstallationsByDate = userRepository.pluginInstallationsByDate(range, granularity, provider)
        return buildUsageBuckets(dayGroups, pluginInstallationsByDate).associateBy { it.date }
    }
}
