package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.domain.model.ClaudeRawModelUsage
import de.neuland.tokendashboard.domain.model.ClaudeTokenCounts
import de.neuland.tokendashboard.domain.model.ClaudeUsageRecord
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.TokenCount

class ClaudeUsageRepositoryAdapter(
    private val jdbi: ClaudeUsageRepository,
) : ClaudeUsageRepositoryPort {
    override fun insertAll(records: List<ClaudeUsageRecord>) {
        if (records.isEmpty()) return
        jdbi.insertAll(records.map { ClaudeUsageRow.fromDomain(it) })
    }

    override fun claudeDailyUsage(range: DayRange): Map<Day, List<ClaudeRawModelUsage>> =
        jdbi
            .aggregateClaudeByDayAndModel(range.from.value, range.to.value)
            .groupBy { it.date }
            .mapKeys { Day(it.key) }
            .mapValues { toRawModelUsages(it.value) }

    private fun toRawModelUsages(dayRows: List<AggregatedClaudeBucketRow>): List<ClaudeRawModelUsage> =
        dayRows
            .map { row ->
                ClaudeRawModelUsage(
                    model = ModelName.restore(row.model),
                    tokens =
                        ClaudeTokenCounts(
                            inputTokens = TokenCount(row.inputTokens),
                            outputTokens = TokenCount(row.outputTokens),
                            cacheReadTokens = TokenCount(row.cacheReadTokens),
                            cacheWriteTokens5m = TokenCount(row.cacheWrite5mTokens),
                            cacheWriteTokens1h = TokenCount(row.cacheWrite1hTokens),
                            cacheWriteTokensGeneric = TokenCount(row.cacheWriteTokens),
                        ),
                )
            }
}
