package de.neuland.tokendashboard.application.port.incoming

import de.neuland.tokendashboard.domain.model.AllProviderUsageSeries
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity

interface QueryAllUsageSeriesPort {
    fun queryAllUsageSeries(
        range: DayRange,
        granularity: Granularity,
    ): AllProviderUsageSeries
}
