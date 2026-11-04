package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.ClaudeRawModelUsage
import de.neuland.tokendashboard.domain.model.ClaudeTokenCounts
import de.neuland.tokendashboard.domain.model.ClaudeUsageRecord
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.IncomingCacheWriteTokens
import de.neuland.tokendashboard.domain.model.IncomingTokenCounts
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptId
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCount
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

        test("claudeDailyUsage maps every token column to its own field and groups rows by day") {
            fun row(
                date: LocalDate,
                model: String,
            ) = AggregatedClaudeBucketRow(
                date = date,
                model = model,
                inputTokens = 1,
                outputTokens = 2,
                cacheWrite5mTokens = 3,
                cacheWrite1hTokens = 4,
                cacheWriteTokens = 5,
                cacheReadTokens = 6,
            )
            every { jdbi.aggregateClaudeByDayAndModel(from.value, to.value) } returns
                listOf(
                    row(to.value, "claude-opus-4-5"),
                    row(to.value, "claude-sonnet-4-5"),
                    row(from.value, "claude-opus-4-5"),
                )

            val result = adapter.claudeDailyUsage(DayRange.of(from, to))

            val tokens =
                ClaudeTokenCounts(
                    inputTokens = TokenCount(1),
                    outputTokens = TokenCount(2),
                    cacheReadTokens = TokenCount(6),
                    cacheWriteTokens5m = TokenCount(3),
                    cacheWriteTokens1h = TokenCount(4),
                    cacheWriteTokensGeneric = TokenCount(5),
                )
            result shouldBe
                mapOf(
                    to to
                        listOf(
                            ClaudeRawModelUsage(ModelName.from("claude-opus-4-5"), tokens),
                            ClaudeRawModelUsage(ModelName.from("claude-sonnet-4-5"), tokens),
                        ),
                    from to listOf(ClaudeRawModelUsage(ModelName.from("claude-opus-4-5"), tokens)),
                )
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
                            cacheWriteTokens =
                                IncomingCacheWriteTokens(
                                    TokenCount.ZERO,
                                    TokenCount.ZERO,
                                    TokenCount(400),
                                ),
                        ),
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
                            cacheWriteTokens =
                                IncomingCacheWriteTokens(
                                    TokenCount(40),
                                    TokenCount(60),
                                    TokenCount.ZERO,
                                ),
                        ),
                    pluginVersion = PluginVersion("1.2.3"),
                )

            adapter.insertAll(listOf(record))

            val row = slot.captured.single()
            row.cacheWrite5mTokens shouldBe 40
            row.cacheWrite1hTokens shouldBe 60
            row.cacheWriteTokens shouldBe 0
        }
    })
