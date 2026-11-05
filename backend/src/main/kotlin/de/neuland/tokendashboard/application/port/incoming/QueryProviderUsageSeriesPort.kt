package de.neuland.tokendashboard.application.port.incoming

import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.UsageSeries

interface QueryProviderUsageSeriesPort {
    fun querySeries(
        provider: Provider,
        range: DayRange,
        granularity: Granularity,
    ): UsageSeries
}
