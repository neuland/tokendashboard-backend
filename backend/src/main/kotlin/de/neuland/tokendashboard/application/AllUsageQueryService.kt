package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.QueryAllUsagePort
import de.neuland.tokendashboard.domain.model.AllProviderTotals
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.ProviderTotalsByProvider
import de.neuland.tokendashboard.domain.model.totals
import de.neuland.tokendashboard.domain.sumByModel

class AllUsageQueryService(
    private val usageSources: ProviderUsageSources,
) : QueryAllUsagePort {
    override fun queryAllUsage(range: DayRange): AllProviderTotals =
        AllProviderTotals(
            range = range,
            totals =
                ProviderTotalsByProvider.forAllProviders { provider ->
                    usageSources.of(provider).dailyUsage(range).sumByModel().totals()
                },
        )
}
