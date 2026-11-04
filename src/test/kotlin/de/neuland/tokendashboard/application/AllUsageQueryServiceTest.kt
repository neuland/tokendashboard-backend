package de.neuland.tokendashboard.application

import arrow.core.nonEmptyListOf
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.DatedModelUsages
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.ModelUsages
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageTotals
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class AllUsageQueryServiceTest :
    FunSpec({
        val from = Day(LocalDate.of(2026, 6, 1))
        val to = Day(LocalDate.of(2026, 6, 15))

        fun modelUsage(
            model: String,
            costUsdCent: Long,
            estimatedCo2: Double,
        ) = ModelUsage(
            model = ModelName.from(model),
            tokens =
                TokenCounts(
                    inputTokens = TokenCount(100),
                    outputTokens = TokenCount(200),
                    cacheReadTokens = TokenCount(1_000),
                    cacheWriteTokens = TokenCount(1_000),
                ),
            costUsdCent = DollarCent(costUsdCent),
            estimatedCO2 = Co2Gram(estimatedCo2),
        )

        fun oneDay(
            model: String,
            costUsdCent: Long,
        ) = listOf(DatedModelUsages(from, ModelUsages.HasUsage(nonEmptyListOf(modelUsage(model, costUsdCent, 1.0)))))

        val claudeSource = mockk<ProviderUsageSource>()
        val copilotSource = mockk<ProviderUsageSource>()
        val openCodeSource = mockk<ProviderUsageSource>()

        every { claudeSource.dailyUsage(any()) } returns emptyList()
        every { copilotSource.dailyUsage(any()) } returns oneDay("gpt-5.4-mini", 2)
        every { openCodeSource.dailyUsage(any()) } returns oneDay("deepseek-v4-pro", 3)

        val service = AllUsageQueryService(ProviderUsageSources(claudeSource, copilotSource, openCodeSource))

        test("query all usage maps each provider to its own totals, with zero totals for a provider without usage") {
            val result = service.queryAllUsage(DayRange.of(from, to))

            result.totals.byProvider.getValue(CLAUDE) shouldBe UsageTotals.zero()
            result.totals.byProvider.getValue(COPILOT).costUsdCent shouldBe DollarCent(2)
            result.totals.byProvider.getValue(OPENCODE).costUsdCent shouldBe DollarCent(3)
        }

        test("query all usage passes through from and to") {
            val result = service.queryAllUsage(DayRange.of(from, to))

            result.range.from shouldBe from
            result.range.to shouldBe to
        }

        test("query all usage sums tokens in and out, cost and co2 over all days and models") {
            every { claudeSource.dailyUsage(any()) } returns
                listOf(
                    DatedModelUsages(
                        to,
                        ModelUsages.HasUsage(
                            nonEmptyListOf(modelUsage("claude-sonnet-4-5", 10, 1.5), modelUsage("claude-opus-4-5", 20, 2.5)),
                        ),
                    ),
                    DatedModelUsages(from, ModelUsages.NoUsage),
                    DatedModelUsages(from, ModelUsages.HasUsage(nonEmptyListOf(modelUsage("claude-sonnet-4-5", 30, 3.0)))),
                )

            val result = service.queryAllUsage(DayRange.of(from, to))

            val claude = result.totals.byProvider.getValue(CLAUDE)
            claude.tokensInOut() shouldBe TokenCount(3 * 300)
            claude.estimatedCO2 shouldBe Co2Gram(1.5 + 2.5 + 3.0)
            claude.costUsdCent shouldBe DollarCent(10 + 20 + 30)
        }
    })
