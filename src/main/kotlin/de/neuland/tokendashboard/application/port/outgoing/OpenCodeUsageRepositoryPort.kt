package de.neuland.tokendashboard.application.port.outgoing

import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.ModelUsagesAtDate
import de.neuland.tokendashboard.domain.model.OpenCodeUsageRecord
import de.neuland.tokendashboard.domain.model.ProviderUsage
import de.neuland.tokendashboard.domain.model.ProviderUsageDetail

interface OpenCodeUsageRepositoryPort {
    fun insertAll(records: List<OpenCodeUsageRecord>)

    fun openCodeProviderUsage(range: DayRange): ProviderUsage

    fun openCodeUsageByDateRange(range: DayRange): ProviderUsageDetail

    fun openCodeUsageSeries(
        range: DayRange,
        granularity: Granularity,
    ): List<ModelUsagesAtDate>
}
