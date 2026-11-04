package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.QueryProviderUsagePort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.ProviderUsageByModel
import de.neuland.tokendashboard.domain.model.ProviderUsageOverview
import de.neuland.tokendashboard.domain.sumByModel

class ProviderUsageQueryService(
    private val usageSources: ProviderUsageSources,
    private val userRepository: UserRepositoryPort,
) : QueryProviderUsagePort {
    override fun queryUsage(
        provider: Provider,
        range: DayRange,
    ): ProviderUsageOverview =
        ProviderUsageOverview(
            usage = ProviderUsageByModel(provider, range, usageSources.of(provider).dailyUsage(range).sumByModel()),
            activeUsersLast4Weeks = userRepository.activeUsersLast4Weeks(provider),
        )
}
