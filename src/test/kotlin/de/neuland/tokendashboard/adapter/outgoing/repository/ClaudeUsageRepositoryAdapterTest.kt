package de.neuland.tokendashboard.adapter.outgoing.repository

import arrow.core.toNonEmptyListOrNull
import de.neuland.tokendashboard.domain.model.ClaudeUsageRecord
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.IncomingCacheWriteTokens
import de.neuland.tokendashboard.domain.model.IncomingTokenCounts
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptId
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
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

class ClaudeUsageRepositoryAdapterTest :
    FunSpec({

        val jdbi = mockk<ClaudeUsageRepository>()
        val adapter = ClaudeUsageRepositoryAdapter(jdbi)

        val from = Day(LocalDate.of(2026, 6, 1))
        val to = Day(LocalDate.of(2026, 6, 15))

        test("claudeProviderUsage maps totals row to ProviderUsage") {
            every { jdbi.aggregateClaudeTotals(from.value, to.value) } returns
                AggregatedClaudeTotalsRow(tokens = 42, costUsdCent = 123L, co2Gram = 5.5)

            val result = adapter.claudeProviderUsage(DayRange.of(from, to))

            result.provider shouldBe CLAUDE
            result.tokensInOut shouldBe TokenCount(42)
            result.totalEstimatedCo2 shouldBe Co2Gram(5.5)
            result.totalCostUsdCent shouldBe DollarCent(123L)
        }

        test("claudeUsageByDateRange maps rows to ModelUsage list") {
            every { jdbi.aggregateClaudeByModel(from.value, to.value) } returns
                listOf(
                    AggregatedClaudeRow(
                        model = "claude-opus-4-5",
                        inputTokens = 100,
                        outputTokens = 200,
                        cacheWrite5mTokens = 0,
                        cacheWrite1hTokens = 0,
                        cacheWriteTokens = 10,
                        cacheReadTokens = 20,
                        costUsdCent = 7L,
                        co2Gram = 1.1,
                    ),
                )

            val result = adapter.claudeUsageByDateRange(DayRange.of(from, to))

            result.provider shouldBe CLAUDE
            result.range.from shouldBe from
            result.range.to shouldBe to
            result.modelUsages shouldBe
                UsageResult.HasUsage(
                    listOf(
                        ModelUsage(
                            model = ModelName.restore("claude-opus-4-5"),
                            tokens =
                                TokenCounts(
                                    inputTokens = TokenCount(100),
                                    outputTokens = TokenCount(200),
                                    cacheReadTokens = TokenCount(20),
                                    cacheWriteTokens = TokenCount(10),
                                ),
                            costUsdCent = DollarCent(7L),
                            estimatedCO2 = Co2Gram(1.1),
                        ),
                    ).toNonEmptyListOrNull()!!,
                )
        }

        test("claudeUsageByDateRange returns empty list when no rows found") {
            every { jdbi.aggregateClaudeByModel(from.value, to.value) } returns emptyList()

            adapter.claudeUsageByDateRange(DayRange.of(from, to)).modelUsages shouldBe UsageResult.NoUsage
        }

        test("insertAll delegates rows built from the domain records") {
            val slot = slot<List<ClaudeUsageRow>>()
            every { jdbi.insertAll(capture(slot)) } just Runs

            val record =
                ClaudeUsageRecord(
                    model = ModelName.from("claude-opus-4-5"),
                    timestamp = PromptTimestamp(Instant.parse("2026-06-14T10:15:00Z")),
                    sessionId = SessionId("session-1"),
                    promptId = PromptId("prompt-1"),
                    tokens =
                        IncomingTokenCounts(
                            inputTokens = TokenCount(100),
                            outputTokens = TokenCount(200),
                            cacheReadTokens = TokenCount(300),
                            cacheWriteTokens = IncomingCacheWriteTokens(TokenCount.ZERO, TokenCount.ZERO, TokenCount(400)),
                        ),
                    costUsdCent = DollarCent(50L),
                    co2Gram = Co2Gram(2.5),
                    pluginVersion = PluginVersion("1.2.3"),
                )

            adapter.insertAll(listOf(record))

            verify(exactly = 1) { jdbi.insertAll(any()) }
            val row = slot.captured.single()
            row.model shouldBe "claude-opus-4-5"
            row.timestamp shouldBe Instant.parse("2026-06-14T10:15:00Z")
            row.promptId shouldBe "prompt-1"
            row.inputTokens shouldBe 100
            row.outputTokens shouldBe 200
            row.cacheReadTokens shouldBe 300
            row.cacheWriteTokens shouldBe 400
            row.costUsdCent shouldBe 50L
            row.co2Gram shouldBe 2.5
            row.pluginVersion shouldBe "1.2.3"
        }

        test("insertAll maps split cache-write tokens to their own columns, leaving the undated column zero") {
            val slot = slot<List<ClaudeUsageRow>>()
            every { jdbi.insertAll(capture(slot)) } just Runs

            val record =
                ClaudeUsageRecord(
                    model = ModelName.from("claude-opus-4-5"),
                    timestamp = PromptTimestamp(Instant.parse("2026-06-14T10:15:00Z")),
                    sessionId = SessionId("session-1"),
                    promptId = PromptId("prompt-1"),
                    tokens =
                        IncomingTokenCounts(
                            inputTokens = TokenCount(100),
                            outputTokens = TokenCount(200),
                            cacheReadTokens = TokenCount(300),
                            cacheWriteTokens = IncomingCacheWriteTokens(TokenCount(40), TokenCount(60), TokenCount.ZERO),
                        ),
                    costUsdCent = DollarCent(50L),
                    co2Gram = Co2Gram(2.5),
                    pluginVersion = PluginVersion("1.2.3"),
                )

            adapter.insertAll(listOf(record))

            val row = slot.captured.single()
            row.cacheWrite5mTokens shouldBe 40
            row.cacheWrite1hTokens shouldBe 60
            row.cacheWriteTokens shouldBe 0
        }
    })
