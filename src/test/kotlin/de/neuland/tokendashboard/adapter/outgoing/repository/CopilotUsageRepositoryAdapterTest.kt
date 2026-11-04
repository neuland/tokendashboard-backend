package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.CopilotRawModelUsage
import de.neuland.tokendashboard.domain.model.CopilotUsageRecord
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.NanoAiu
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
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

        test("copilotDailyUsage maps day rows to tokens and nanoAiu per model, grouped by day") {
            fun row(
                date: LocalDate,
                model: String,
            ) = AggregatedCopilotBucketRow(
                date = date,
                model = model,
                inputTokens = 100,
                outputTokens = 200,
                cacheReadTokens = 20,
                cacheWriteTokens = 10,
                nanoAiu = 1_500_000_000L,
            )
            every { jdbi.aggregateCopilotByDayAndModel(from.value, to.value) } returns
                listOf(row(to.value, "gpt-5.4"), row(to.value, "gpt-5.4-mini"), row(from.value, "gpt-5.4-mini"))

            val result = adapter.copilotDailyUsage(DayRange.of(from, to))

            fun tokens(model: String) =
                CopilotRawModelUsage(
                    model = ModelName.restore(model),
                    tokens = TokenCounts(TokenCount(100), TokenCount(200), TokenCount(20), TokenCount(10)),
                    nanoAiu = NanoAiu(1_500_000_000L),
                )
            result shouldBe
                mapOf(
                    to to listOf(tokens("gpt-5.4"), tokens("gpt-5.4-mini")),
                    from to listOf(tokens("gpt-5.4-mini")),
                )
        }

        test("copilotDailyUsage returns no days when no rows found") {
            every { jdbi.aggregateCopilotByDayAndModel(from.value, to.value) } returns emptyList()

            adapter.copilotDailyUsage(DayRange.of(from, to)) shouldBe emptyMap()
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
            row.pluginVersion shouldBe "1.2.3"
        }
    })
