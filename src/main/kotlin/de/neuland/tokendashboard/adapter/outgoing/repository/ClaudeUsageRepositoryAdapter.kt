package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.domain.model.ClaudeUsageRecord
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.ModelUsagesAtDate
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.ProviderUsage
import de.neuland.tokendashboard.domain.model.ProviderUsageDetail
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageResult
import de.neuland.tokendashboard.domain.model.toUsageResult

class ClaudeUsageRepositoryAdapter(
    private val jdbi: ClaudeUsageRepository,
) : ClaudeUsageRepositoryPort {
    override fun insertAll(records: List<ClaudeUsageRecord>) {
        if (records.isEmpty()) return
        jdbi.insertAll(records.map { ClaudeUsageRow.fromDomain(it) })
    }

    override fun claudeProviderUsage(range: DayRange): ProviderUsage {
        val row = jdbi.aggregateClaudeTotals(range.from.value, range.to.value)
        return ProviderUsage(
            provider = CLAUDE,
            tokensInOut = TokenCount(row.tokens),
            totalEstimatedCo2 = Co2Gram(row.co2Gram),
            totalCostUsdCent = DollarCent(row.costUsdCent),
        )
    }

    override fun claudeUsageByDateRange(range: DayRange): ProviderUsageDetail {
        val rows = jdbi.aggregateClaudeByModel(range.from.value, range.to.value)
        val modelUsages =
            rows.map { row ->
                ModelUsage(
                    model = ModelName.restore(row.model),
                    tokens =
                        TokenCounts(
                            inputTokens = TokenCount(row.inputTokens),
                            outputTokens = TokenCount(row.outputTokens),
                            cacheWriteTokens = TokenCount(row.cacheWriteTokens + row.cacheWrite5mTokens + row.cacheWrite1hTokens),
                            cacheReadTokens = TokenCount(row.cacheReadTokens),
                        ),
                    costUsdCent = DollarCent(row.costUsdCent),
                    estimatedCO2 = Co2Gram(row.co2Gram),
                )
            }
        return ProviderUsageDetail(
            provider = CLAUDE,
            range = range,
            modelUsages = modelUsages.toUsageResult(),
        )
    }

    override fun claudeUsageSeries(
        range: DayRange,
        granularity: Granularity,
    ): List<ModelUsagesAtDate> {
        val rows =
            when (granularity) {
                Granularity.DAY -> jdbi.aggregateClaudeByDayAndModel(range.from.value, range.to.value)
                Granularity.WEEK -> jdbi.aggregateClaudeByWeekAndModel(range.from.value, range.to.value)
            }
        return rows
            .groupBy { it.date }
            .map { (date, dayRows) ->
                ModelUsagesAtDate(
                    date = Day(date),
                    modelUsages = toUsageResult(dayRows),
                )
            }.sortedByDescending { it.date.value }
    }

    private fun toUsageResult(dayRows: List<AggregatedClaudeBucketRow>): UsageResult {
        val list =
            dayRows
                .map { row ->
                    ModelUsage(
                        model = ModelName.restore(row.model),
                        tokens =
                            TokenCounts(
                                inputTokens = TokenCount(row.inputTokens),
                                outputTokens = TokenCount(row.outputTokens),
                                cacheReadTokens = TokenCount(row.cacheReadTokens),
                                cacheWriteTokens = TokenCount(row.cacheWriteTokens + row.cacheWrite5mTokens + row.cacheWrite1hTokens),
                            ),
                        costUsdCent = DollarCent(row.costUsdCent),
                        estimatedCO2 = Co2Gram(row.co2Gram),
                    )
                }
        return list.toUsageResult()
    }
}
