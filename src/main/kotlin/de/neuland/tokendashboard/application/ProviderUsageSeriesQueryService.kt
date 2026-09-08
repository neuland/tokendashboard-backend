package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.QueryProviderUsageSeriesPort
import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.buildUsageBuckets
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.UsageSeries

class ProviderUsageSeriesQueryService(
    private val claudeUsageRepository: ClaudeUsageRepositoryPort,
    private val copilotUsageRepository: CopilotUsageRepositoryPort,
    private val openCodeUsageRepository: OpenCodeUsageRepositoryPort,
    private val userRepository: UserRepositoryPort,
) : QueryProviderUsageSeriesPort {
    override fun querySeries(
        provider: Provider,
        range: DayRange,
        granularity: Granularity,
    ): UsageSeries {
        val dayGroups =
            when (provider) {
                CLAUDE -> claudeUsageRepository.claudeUsageSeries(range, granularity)
                COPILOT -> copilotUsageRepository.copilotUsageSeries(range, granularity)
                OPENCODE -> openCodeUsageRepository.openCodeUsageSeries(range, granularity)
            }
        val pluginInstallationsByDate = userRepository.pluginInstallationsByDate(range, granularity, provider)

        return UsageSeries(range, buildUsageBuckets(dayGroups, pluginInstallationsByDate))
    }
}
