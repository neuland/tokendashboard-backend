package de.neuland.tokendashboard.application.port.outgoing

import de.neuland.tokendashboard.domain.model.CopilotUsageRecord
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.ModelUsagesAtDate
import de.neuland.tokendashboard.domain.model.ProviderUsage
import de.neuland.tokendashboard.domain.model.ProviderUsageDetail

interface CopilotUsageRepositoryPort {
    fun insertAll(records: List<CopilotUsageRecord>)

    fun copilotUsageByDateRange(range: DayRange): ProviderUsageDetail

    fun copilotProviderUsage(range: DayRange): ProviderUsage

    fun copilotUsageSeries(
        range: DayRange,
        granularity: Granularity,
    ): List<ModelUsagesAtDate>
}
