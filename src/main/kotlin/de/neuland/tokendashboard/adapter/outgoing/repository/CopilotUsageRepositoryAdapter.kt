package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.CopilotUsageRecord
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.ModelUsagesAtDate
import de.neuland.tokendashboard.domain.model.NanoAiu
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.ProviderUsage
import de.neuland.tokendashboard.domain.model.ProviderUsageDetail
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageResult
import de.neuland.tokendashboard.domain.model.toUsageResult

class CopilotUsageRepositoryAdapter(
    private val jdbi: CopilotUsageRepository,
) : CopilotUsageRepositoryPort {
    override fun insertAll(records: List<CopilotUsageRecord>) {
        if (records.isEmpty()) return
        jdbi.insertAll(records.map { CopilotUsageRow.fromDomain(it) })
    }

    override fun copilotProviderUsage(range: DayRange): ProviderUsage {
        val row = jdbi.aggregateCopilotTotals(range.from.value, range.to.value)
        return ProviderUsage(
            provider = COPILOT,
            tokensInOut = TokenCount(row.tokens),
            totalEstimatedCo2 = Co2Gram(row.co2Gram),
            totalCostUsdCent = DollarCent(NanoAiu(row.nanoAiu).toAiu()),
        )
    }

    override fun copilotUsageByDateRange(range: DayRange): ProviderUsageDetail {
        val rows = jdbi.aggregateCopilotByModel(range.from.value, range.to.value)
        val modelUsages =
            rows.map { row ->
                ModelUsage(
                    model = ModelName.restore(row.model),
                    tokens =
                        TokenCounts(
                            inputTokens = TokenCount(row.inputTokens),
                            outputTokens = TokenCount(row.outputTokens),
                            cacheReadTokens = TokenCount(row.cacheReadTokens),
                            cacheWriteTokens = TokenCount(row.cacheWriteTokens),
                        ),
                    costUsdCent = DollarCent(NanoAiu(row.nanoAiu).toAiu()),
                    estimatedCO2 = Co2Gram(row.co2Gram),
                )
            }
        return ProviderUsageDetail(
            provider = COPILOT,
            range = range,
            modelUsages = modelUsages.toUsageResult(),
        )
    }

    override fun copilotUsageSeries(
        range: DayRange,
        granularity: Granularity,
    ): List<ModelUsagesAtDate> {
        val rows =
            when (granularity) {
                Granularity.DAY -> jdbi.aggregateCopilotByDayAndModel(range.from.value, range.to.value)
                Granularity.WEEK -> jdbi.aggregateCopilotByWeekAndModel(range.from.value, range.to.value)
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

    private fun toUsageResult(dayRows: List<AggregatedCopilotBucketRow>): UsageResult {
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
                                cacheWriteTokens = TokenCount(row.cacheWriteTokens),
                            ),
                        costUsdCent = DollarCent(NanoAiu(row.nanoAiu).toAiu()),
                        estimatedCO2 = Co2Gram(row.co2Gram),
                    )
                }
        return list.toUsageResult()
    }
}
