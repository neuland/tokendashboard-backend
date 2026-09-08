package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.ModelUsagesAtDate
import de.neuland.tokendashboard.domain.model.NanoCent
import de.neuland.tokendashboard.domain.model.OpenCodeUsageRecord
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.ProviderUsage
import de.neuland.tokendashboard.domain.model.ProviderUsageDetail
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageResult
import de.neuland.tokendashboard.domain.model.toUsageResult

class OpenCodeUsageRepositoryAdapter(
    private val jdbi: OpenCodeUsageRepository,
) : OpenCodeUsageRepositoryPort {
    override fun insertAll(records: List<OpenCodeUsageRecord>) {
        if (records.isEmpty()) return
        jdbi.insertAll(records.map { OpenCodeUsageRow.fromDomain(it) })
    }

    override fun openCodeProviderUsage(range: DayRange): ProviderUsage {
        val openCodeRow = jdbi.aggregateOpenCodeTotals(range.from.value, range.to.value)
        return ProviderUsage(
            provider = OPENCODE,
            tokensInOut = TokenCount(openCodeRow.tokens),
            totalEstimatedCo2 = Co2Gram(openCodeRow.co2Gram),
            totalCostUsdCent = NanoCent(openCodeRow.costNanoCent).toCent(),
        )
    }

    override fun openCodeUsageByDateRange(range: DayRange): ProviderUsageDetail {
        val rows = jdbi.aggregateOpenCodeByModel(range.from.value, range.to.value)
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
                    costUsdCent = NanoCent(row.costNanoCent).toCent(),
                    estimatedCO2 = Co2Gram(row.co2Gram),
                )
            }
        return ProviderUsageDetail(
            provider = OPENCODE,
            range = range,
            modelUsages = modelUsages.toUsageResult(),
        )
    }

    override fun openCodeUsageSeries(
        range: DayRange,
        granularity: Granularity,
    ): List<ModelUsagesAtDate> {
        val rows =
            when (granularity) {
                Granularity.DAY -> jdbi.aggregateOpenCodeByDayAndModel(range.from.value, range.to.value)
                Granularity.WEEK -> jdbi.aggregateOpenCodeByWeekAndModel(range.from.value, range.to.value)
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

    private fun toUsageResult(dayRows: List<AggregatedOpenCodeBucketRow>): UsageResult {
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
                        costUsdCent = NanoCent(row.costNanoCent).toCent(),
                        estimatedCO2 = Co2Gram(row.co2Gram),
                    )
                }
        return list.toUsageResult()
    }
}
