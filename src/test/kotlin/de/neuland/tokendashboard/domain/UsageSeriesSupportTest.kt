package de.neuland.tokendashboard.domain

import arrow.core.toNonEmptyListOrNull
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.ModelUsagesAtDate
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageResult
import de.neuland.tokendashboard.domain.model.UserCount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class UsageSeriesSupportTest :
    FunSpec({
        val june14 = Day(LocalDate.of(2026, 6, 14))
        val june15 = Day(LocalDate.of(2026, 6, 15))

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

        fun usages(model: String) = UsageResult.HasUsage(listOf(modelUsage(model)).toNonEmptyListOrNull()!!)

        test("matches plugin installations by date, not by position") {
            val dayGroups = listOf(ModelUsagesAtDate(june14, usages("sonnet")), ModelUsagesAtDate(june15, usages("opus")))
            // insertion order deliberately reversed relative to dayGroups
            val installations = mapOf(june15 to UserCount(5), june14 to UserCount(3))

            val buckets = buildUsageBuckets(dayGroups, installations)

            buckets.first { it.date == june14 }.pluginInstallations shouldBe UserCount(3)
            buckets.first { it.date == june15 }.pluginInstallations shouldBe UserCount(5)
        }

        test("falls back to zero installations when a date is missing from the map") {
            val buckets = buildUsageBuckets(listOf(ModelUsagesAtDate(june14, usages("sonnet"))), emptyMap())

            buckets.single().pluginInstallations shouldBe UserCount(0)
        }

        test("keeps the order and dates of the day groups") {
            val dayGroups = listOf(ModelUsagesAtDate(june15, usages("opus")), ModelUsagesAtDate(june14, usages("sonnet")))

            val buckets = buildUsageBuckets(dayGroups, mapOf(june14 to UserCount(3), june15 to UserCount(5)))

            buckets.map { it.date } shouldBe listOf(june15, june14)
        }

        test("ignores installation entries with no matching day group") {
            val buckets =
                buildUsageBuckets(
                    listOf(ModelUsagesAtDate(june14, usages("sonnet"))),
                    mapOf(june14 to UserCount(3), june15 to UserCount(99)),
                )

            buckets.map { it.date } shouldBe listOf(june14)
        }
    })
