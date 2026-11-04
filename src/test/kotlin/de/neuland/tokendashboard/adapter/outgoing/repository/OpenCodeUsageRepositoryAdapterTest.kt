package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.LlmProviderName
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.NanoCent
import de.neuland.tokendashboard.domain.model.OpenCodeRawModelUsage
import de.neuland.tokendashboard.domain.model.OpenCodeUsageRecord
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptId
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

class OpenCodeUsageRepositoryAdapterTest :
    FunSpec({

        val jdbi = mockk<OpenCodeUsageRepository>()
        val adapter = OpenCodeUsageRepositoryAdapter(jdbi)

        val from = Day(LocalDate.of(2026, 6, 1))
        val to = Day(LocalDate.of(2026, 6, 15))

        test("openCodeDailyUsage maps day rows to tokens and cost per model, grouped by day") {
            fun row(
                date: LocalDate,
                model: String,
            ) = AggregatedOpenCodeBucketRow(
                date = date,
                model = model,
                inputTokens = 100,
                outputTokens = 200,
                cacheWriteTokens = 10,
                cacheReadTokens = 20,
                costNanoCent = 5_000_000_000L,
            )
            every { jdbi.aggregateOpenCodeByDayAndModel(from.value, to.value) } returns
                listOf(row(to.value, "deepseek-v4-pro"), row(to.value, "mimo-v2.5-free"), row(from.value, "deepseek-v4-pro"))

            val result = adapter.openCodeDailyUsage(DayRange.of(from, to))

            fun tokens(model: String) =
                OpenCodeRawModelUsage(
                    model = ModelName.restore(model),
                    tokens = TokenCounts(TokenCount(100), TokenCount(200), TokenCount(20), TokenCount(10)),
                    cost = NanoCent(5_000_000_000L),
                )
            result shouldBe
                mapOf(
                    to to listOf(tokens("deepseek-v4-pro"), tokens("mimo-v2.5-free")),
                    from to listOf(tokens("deepseek-v4-pro")),
                )
        }

        test("openCodeDailyUsage returns no days when no rows found") {
            every { jdbi.aggregateOpenCodeByDayAndModel(from.value, to.value) } returns emptyList()

            adapter.openCodeDailyUsage(DayRange.of(from, to)) shouldBe emptyMap()
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
            row.pluginVersion shouldBe "1.2.3"
        }
    })
