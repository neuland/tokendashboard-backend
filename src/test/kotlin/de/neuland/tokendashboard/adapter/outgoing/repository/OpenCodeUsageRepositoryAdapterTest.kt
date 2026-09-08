package de.neuland.tokendashboard.adapter.outgoing.repository

import arrow.core.toNonEmptyListOrNull
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.LlmProviderName
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.NanoCent
import de.neuland.tokendashboard.domain.model.OpenCodeUsageRecord
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptId
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
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

class OpenCodeUsageRepositoryAdapterTest :
    FunSpec({

        val jdbi = mockk<OpenCodeUsageRepository>()
        val adapter = OpenCodeUsageRepositoryAdapter(jdbi)

        val from = Day(LocalDate.of(2026, 6, 1))
        val to = Day(LocalDate.of(2026, 6, 15))

        test("openCodeProviderUsage maps totals row to ProviderUsage") {
            every { jdbi.aggregateOpenCodeTotals(from.value, to.value) } returns
                AggregatedOpenCodeTotalsRow(tokens = 42, costNanoCent = 123_000_000_000L, co2Gram = 5.5)

            val result = adapter.openCodeProviderUsage(DayRange.of(from, to))

            result.provider shouldBe OPENCODE
            result.tokensInOut shouldBe TokenCount(42)
            result.totalEstimatedCo2 shouldBe Co2Gram(5.5)
            result.totalCostUsdCent.value shouldBe 123L
        }

        test("openCodeProviderUsage rounds cost instead of truncating") {
            every { jdbi.aggregateOpenCodeTotals(from.value, to.value) } returns
                AggregatedOpenCodeTotalsRow(tokens = 0, costNanoCent = 12_900_000_000L, co2Gram = 0.0)

            val result = adapter.openCodeProviderUsage(DayRange.of(from, to))

            // 12.9 nano-cent-scaled cents -> must round to 13, not truncate to 12
            result.totalCostUsdCent.value shouldBe 13L
        }

        test("openCodeUsageByDateRange maps multiple rows to ModelUsage list") {
            every { jdbi.aggregateOpenCodeByModel(from.value, to.value) } returns
                listOf(
                    AggregatedOpenCodeRow(
                        model = "deepseek-v4-pro",
                        inputTokens = 100,
                        outputTokens = 200,
                        cacheWriteTokens = 10,
                        cacheReadTokens = 20,
                        costNanoCent = 5_000_000_000L,
                        co2Gram = 1.1,
                    ),
                    AggregatedOpenCodeRow(
                        model = "mimo-v2.5-free",
                        inputTokens = 50,
                        outputTokens = 60,
                        cacheWriteTokens = 1,
                        cacheReadTokens = 2,
                        costNanoCent = 0L,
                        co2Gram = 0.0,
                    ),
                )

            val result = adapter.openCodeUsageByDateRange(DayRange.of(from, to))

            result.provider shouldBe OPENCODE
            result.range.from shouldBe from
            result.range.to shouldBe to
            result.modelUsages shouldBe
                UsageResult.HasUsage(
                    listOf(
                        ModelUsage(
                            model = ModelName.restore("deepseek-v4-pro"),
                            tokens =
                                TokenCounts(
                                    inputTokens = TokenCount(100),
                                    outputTokens = TokenCount(200),
                                    cacheReadTokens = TokenCount(20),
                                    cacheWriteTokens = TokenCount(10),
                                ),
                            costUsdCent = DollarCent(5L),
                            estimatedCO2 = Co2Gram(1.1),
                        ),
                        ModelUsage(
                            model = ModelName.restore("mimo-v2.5-free"),
                            tokens =
                                TokenCounts(
                                    inputTokens = TokenCount(50),
                                    outputTokens = TokenCount(60),
                                    cacheReadTokens = TokenCount(2),
                                    cacheWriteTokens = TokenCount(1),
                                ),
                            costUsdCent = DollarCent(0L),
                            estimatedCO2 = Co2Gram(0.0),
                        ),
                    ).toNonEmptyListOrNull()!!,
                )
        }

        test("openCodeUsageByDateRange returns empty list when no rows found") {
            every { jdbi.aggregateOpenCodeByModel(from.value, to.value) } returns emptyList()

            val result = adapter.openCodeUsageByDateRange(DayRange.of(from, to))

            result.modelUsages shouldBe UsageResult.NoUsage
        }

        test("insertAll delegates rows built from the domain records, converting dollars to nano-cent") {
            val slot = slot<List<OpenCodeUsageRow>>()
            every { jdbi.insertAll(capture(slot)) } just Runs

            val record =
                OpenCodeUsageRecord(
                    model = ModelName.from("deepseek-v4-pro"),
                    llmProvider = LlmProviderName("deepseek"),
                    timestamp = PromptTimestamp(Instant.parse("2026-06-14T10:15:00Z")),
                    sessionId = SessionId("session-1"),
                    promptId = PromptId("prompt-1"),
                    tokens =
                        TokenCounts(
                            inputTokens = TokenCount(100),
                            outputTokens = TokenCount(200),
                            cacheReadTokens = TokenCount(300),
                            cacheWriteTokens = TokenCount(400),
                        ),
                    costNanoCent = NanoCent.fromDollar(1.5),
                    co2Gram = Co2Gram(2.5),
                    pluginVersion = PluginVersion("1.2.3"),
                )

            adapter.insertAll(listOf(record))

            verify(exactly = 1) { jdbi.insertAll(any()) }
            val row = slot.captured.single()
            row.model shouldBe "deepseek-v4-pro"
            row.llmProvider shouldBe "deepseek"
            row.timestamp shouldBe Instant.parse("2026-06-14T10:15:00Z")
            row.promptId shouldBe "prompt-1"
            row.inputTokens shouldBe 100
            row.outputTokens shouldBe 200
            row.cacheReadTokens shouldBe 300
            row.cacheWriteTokens shouldBe 400
            // 1.5 dollar = 150 cent = 150_000_000_000 nano-cent
            row.costNanoCent shouldBe 150_000_000_000L
            row.co2Gram shouldBe 2.5
            row.pluginVersion shouldBe "1.2.3"
        }
    })
