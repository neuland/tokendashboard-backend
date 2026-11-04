package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.domain.model.CopilotRawModelUsage
import de.neuland.tokendashboard.domain.model.CopilotUsageRecord
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.NanoAiu
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts

class CopilotUsageRepositoryAdapter(
    private val jdbi: CopilotUsageRepository,
) : CopilotUsageRepositoryPort {
    override fun insertAll(records: List<CopilotUsageRecord>) {
        if (records.isEmpty()) return
        jdbi.insertAll(records.map { CopilotUsageRow.fromDomain(it) })
    }

    override fun copilotDailyUsage(range: DayRange): Map<Day, List<CopilotRawModelUsage>> =
        jdbi
            .aggregateCopilotByDayAndModel(range.from.value, range.to.value)
            .groupBy { Day(it.date) }
            .mapValues { (_, dayRows) -> dayRows.map { it.toDomain() } }

    private fun AggregatedCopilotBucketRow.toDomain() =
        CopilotRawModelUsage(
            model = ModelName.restore(model),
            tokens =
                TokenCounts(
                    inputTokens = TokenCount(inputTokens),
                    outputTokens = TokenCount(outputTokens),
                    cacheReadTokens = TokenCount(cacheReadTokens),
                    cacheWriteTokens = TokenCount(cacheWriteTokens),
                ),
            nanoAiu = NanoAiu(nanoAiu),
        )
}
