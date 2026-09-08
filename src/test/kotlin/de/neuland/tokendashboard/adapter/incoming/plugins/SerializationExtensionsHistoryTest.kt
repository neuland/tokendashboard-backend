package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageBucket
import de.neuland.tokendashboard.domain.model.UsageSeries
import de.neuland.tokendashboard.domain.model.UserCount
import de.neuland.tokendashboard.domain.model.toUsageResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class SerializationExtensionsHistoryTest :
    FunSpec({
        fun modelUsage(model: String) =
            ModelUsage(
                model = ModelName.from(model),
                tokens =
                    TokenCounts(
                        inputTokens = TokenCount(100),
                        outputTokens = TokenCount(200),
                        cacheReadTokens = TokenCount(10),
                        cacheWriteTokens = TokenCount(20),
                    ),
                costUsdCent = DollarCent(50),
                estimatedCO2 = Co2Gram(5.0),
            )

        test("maps bucket date, model breakdown and totals") {
            val bucket =
                UsageBucket(
                    date = Day(LocalDate.of(2026, 6, 15)),
                    modelUsages = listOf(modelUsage("claude-sonnet-4-5")).toUsageResult(),
                    pluginInstallations = UserCount(3),
                )

            val dto = bucket.toResponse(CLAUDE)

            dto.date shouldBe "2026-06-15"
            dto.modelFamilies shouldHaveSize 1
            val family = dto.modelFamilies.single()
            family.modelFamily shouldBe "sonnet"
            family.models shouldHaveSize 1
            family.models.single().model shouldBe "sonnet-4.5"
            dto.overallTotalTokens.inputTokens shouldBe 100
            dto.overallTotalTokens.outputTokens shouldBe 200
            dto.overallTotalTokens.cacheReadTokens shouldBe 10
            dto.overallTotalTokens.cacheWriteTokens shouldBe 20
            dto.overallTotalTokens.costUsdCent shouldBe 50
            dto.overallTotalTokens.estimatedCo2 shouldBe 5.0
            dto.pluginInstallations shouldBe 3
        }

        test("echoes the requested range on the series response") {
            val range = DayRange.of(Day(LocalDate.of(2026, 6, 1)), Day(LocalDate.of(2026, 6, 30)))
            val series = UsageSeries(range = range, buckets = emptyList())

            val dto = series.toResponse(CLAUDE)

            dto.history.from shouldBe "2026-06-01"
            dto.history.to shouldBe "2026-06-30"
        }

        // Sparse-series contract: days/buckets without usage are omitted rather than
        // appearing as zero-filled entries.
        test("maps an empty series to an empty bucket list") {
            val range = DayRange.of(Day(LocalDate.of(2026, 6, 1)), Day(LocalDate.of(2026, 6, 30)))
            val series = UsageSeries(range = range, buckets = emptyList())

            series.toResponse(CLAUDE).history.provider.buckets.shouldBeEmpty()
        }
    })
