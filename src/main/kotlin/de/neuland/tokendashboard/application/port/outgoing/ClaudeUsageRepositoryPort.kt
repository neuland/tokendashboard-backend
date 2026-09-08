package de.neuland.tokendashboard.application.port.outgoing

import de.neuland.tokendashboard.domain.model.ClaudeUsageRecord
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.ModelUsagesAtDate
import de.neuland.tokendashboard.domain.model.ProviderUsage
import de.neuland.tokendashboard.domain.model.ProviderUsageDetail

interface ClaudeUsageRepositoryPort {
    fun insertAll(records: List<ClaudeUsageRecord>)

    fun claudeProviderUsage(range: DayRange): ProviderUsage

    fun claudeUsageByDateRange(range: DayRange): ProviderUsageDetail

    fun claudeUsageSeries(
        range: DayRange,
        granularity: Granularity,
    ): List<ModelUsagesAtDate>
}
