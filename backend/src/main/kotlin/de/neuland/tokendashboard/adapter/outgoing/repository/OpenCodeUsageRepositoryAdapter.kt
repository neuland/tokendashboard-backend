package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.NanoCent
import de.neuland.tokendashboard.domain.model.OpenCodeRawModelUsage
import de.neuland.tokendashboard.domain.model.OpenCodeUsageRecord
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts

class OpenCodeUsageRepositoryAdapter(
    private val jdbi: OpenCodeUsageRepository,
) : OpenCodeUsageRepositoryPort {
    override fun insertAll(records: List<OpenCodeUsageRecord>) {
        if (records.isEmpty()) return
        jdbi.insertAll(records.map { OpenCodeUsageRow.fromDomain(it) })
    }

    override fun openCodeDailyUsage(range: DayRange): Map<Day, List<OpenCodeRawModelUsage>> =
        jdbi
            .aggregateOpenCodeByDayAndModel(range.from.value, range.to.value)
            .groupBy { Day(it.date) }
            .mapValues { (_, dayRows) -> dayRows.map { it.toDomain() } }

    private fun AggregatedOpenCodeBucketRow.toDomain() =
        OpenCodeRawModelUsage(
            model = ModelName.restore(model),
            tokens =
                TokenCounts(
                    inputTokens = TokenCount(inputTokens),
                    outputTokens = TokenCount(outputTokens),
                    cacheReadTokens = TokenCount(cacheReadTokens),
                    cacheWriteTokens = TokenCount(cacheWriteTokens),
                ),
            cost = NanoCent(costNanoCent),
        )
}
