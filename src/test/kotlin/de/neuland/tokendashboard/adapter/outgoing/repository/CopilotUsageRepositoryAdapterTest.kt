package de.neuland.tokendashboard.adapter.outgoing.repository

import arrow.core.toNonEmptyListOrNull
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.CopilotUsageRecord
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.NanoAiu
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate

class CopilotUsageRepositoryAdapterTest :
    FunSpec({

        val jdbi = mockk<CopilotUsageRepository>()
        val adapter = CopilotUsageRepositoryAdapter(jdbi)

        val from = Day(LocalDate.of(2026, 6, 1))
        val to = Day(LocalDate.of(2026, 6, 15))

        test("copilotProviderUsage maps totals row to ProviderUsage, converting nanoAiu to cent") {
            every { jdbi.aggregateCopilotTotals(from.value, to.value) } returns
                AggregatedCopilotTotalsRow(tokens = 42, nanoAiu = 1_500_000_000L, co2Gram = 5.5)

            val result = adapter.copilotProviderUsage(DayRange.of(from, to))

            result.provider shouldBe COPILOT
            result.tokensInOut shouldBe TokenCount(42)
            result.totalEstimatedCo2 shouldBe Co2Gram(5.5)
            // 1.5 aiu rounds up to 2, not truncated to 1
            result.totalCostUsdCent shouldBe DollarCent(2L)
        }

        test("copilotProviderUsage rounds sub-aiu remainder instead of truncating") {
            every { jdbi.aggregateCopilotTotals(from.value, to.value) } returns
                AggregatedCopilotTotalsRow(tokens = 0, nanoAiu = 999_999_999L, co2Gram = 0.0)

            val result = adapter.copilotProviderUsage(DayRange.of(from, to))

            // 999_999_999 nanoAiu is one unit short of 1 aiu -> rounds up to 1, not truncated to 0
            result.totalCostUsdCent shouldBe DollarCent(1L)
        }

        test("copilotUsageByDateRange maps rows to ModelUsage list") {
            every { jdbi.aggregateCopilotByModel(from.value, to.value) } returns
                listOf(
                    AggregatedCopilotRow(
                        model = "gpt-5.4-mini",
                        inputTokens = 100,
                        outputTokens = 200,
                        cacheReadTokens = 20,
                        cacheWriteTokens = 10,
                        nanoAiu = 2_000_000_000L,
                        co2Gram = 1.1,
                    ),
                )

            val result = adapter.copilotUsageByDateRange(DayRange.of(from, to))

            result.provider shouldBe COPILOT
            result.range.from shouldBe from
            result.range.to shouldBe to
            result.modelUsages shouldBe
                UsageResult.HasUsage(
                    listOf(
                        ModelUsage(
                            model = ModelName.restore("gpt-5.4-mini"),
                            tokens =
                                TokenCounts(
                                    inputTokens = TokenCount(100),
                                    outputTokens = TokenCount(200),
                                    cacheReadTokens = TokenCount(20),
                                    cacheWriteTokens = TokenCount(10),
                                ),
                            costUsdCent = DollarCent(2L),
                            estimatedCO2 = Co2Gram(1.1),
                        ),
                    ).toNonEmptyListOrNull()!!,
                )
        }

        test("copilotUsageByDateRange returns empty list when no rows found") {
            every { jdbi.aggregateCopilotByModel(from.value, to.value) } returns emptyList()

            adapter.copilotUsageByDateRange(DayRange.of(from, to)).modelUsages shouldBe UsageResult.NoUsage
        }

        test("insertAll delegates rows built from the domain records") {
            val slot = slot<List<CopilotUsageRow>>()
            every { jdbi.insertAll(capture(slot)) } just Runs

            val record =
                CopilotUsageRecord(
                    model = ModelName.from("gpt-5.4-mini"),
                    timestamp = PromptTimestamp(Instant.parse("2026-06-14T10:15:00Z")),
                    sessionId = SessionId("session-1"),
                    tokens =
                        TokenCounts(
                            inputTokens = TokenCount(100),
                            outputTokens = TokenCount(200),
                            cacheReadTokens = TokenCount(300),
                            cacheWriteTokens = TokenCount(400),
                        ),
                    nanoAiu = NanoAiu(123456789L),
                    co2Gram = Co2Gram(2.5),
                    pluginVersion = PluginVersion("1.2.3"),
                )

            adapter.insertAll(listOf(record))

            verify(exactly = 1) { jdbi.insertAll(any()) }
            val row = slot.captured.single()
            row.model shouldBe "gpt-5.4-mini"
            row.timestamp shouldBe Instant.parse("2026-06-14T10:15:00Z")
            row.inputTokens shouldBe 100
            row.outputTokens shouldBe 200
            row.cacheReadTokens shouldBe 300
            row.cacheWriteTokens shouldBe 400
            row.nanoAiu shouldBe 123456789L
            row.co2Gram shouldBe 2.5
            row.pluginVersion shouldBe "1.2.3"
        }
    })
