package de.neuland.tokendashboard.domain

import arrow.core.toNonEmptyListOrNull
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.DatedModelUsages
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.ModelUsages
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UserCount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class UsageAggregationTest :
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

        fun usages(model: String) = ModelUsages.HasUsage(listOf(modelUsage(model)).toNonEmptyListOrNull()!!)

        test("matches plugin installations by date, not by position") {
            val dayGroups = listOf(DatedModelUsages(june14, usages("sonnet")), DatedModelUsages(june15, usages("opus")))
            // insertion order deliberately reversed relative to dayGroups
            val installations = mapOf(june15 to UserCount(5), june14 to UserCount(3))

            val buckets = buildUsageBuckets(dayGroups, installations)

            buckets.first { it.date == june14 }.pluginInstallations shouldBe UserCount(3)
            buckets.first { it.date == june15 }.pluginInstallations shouldBe UserCount(5)
        }

        test("falls back to zero installations when a date is missing from the map") {
            val buckets = buildUsageBuckets(listOf(DatedModelUsages(june14, usages("sonnet"))), emptyMap())

            buckets.single().pluginInstallations shouldBe UserCount(0)
        }

        test("keeps the order and dates of the day groups") {
            val dayGroups = listOf(DatedModelUsages(june15, usages("opus")), DatedModelUsages(june14, usages("sonnet")))

            val buckets = buildUsageBuckets(dayGroups, mapOf(june14 to UserCount(3), june15 to UserCount(5)))

            buckets.map { it.date } shouldBe listOf(june15, june14)
        }

        test("ignores installation entries with no matching day group") {
            val buckets =
                buildUsageBuckets(
                    listOf(DatedModelUsages(june14, usages("sonnet"))),
                    mapOf(june14 to UserCount(3), june15 to UserCount(99)),
                )

            buckets.map { it.date } shouldBe listOf(june14)
        }

        test("roll up by DAY keeps one group per day, newest first") {
            val dayGroups = listOf(DatedModelUsages(june14, usages("sonnet")), DatedModelUsages(june15, usages("opus")))

            dayGroups.rollUp(Granularity.DAY) shouldBe
                listOf(DatedModelUsages(june15, usages("opus")), DatedModelUsages(june14, usages("sonnet")))
        }

        test("roll up by WEEK merges days of the same week into one group keyed by monday") {
            val june16 = Day(LocalDate.of(2026, 6, 16))
            val dayGroups = listOf(DatedModelUsages(june15, usages("sonnet")), DatedModelUsages(june16, usages("sonnet")))

            dayGroups.rollUp(Granularity.WEEK) shouldBe
                listOf(
                    DatedModelUsages(
                        june15,
                        ModelUsages.HasUsage(
                            listOf(
                                ModelUsage(
                                    model = ModelName.from("sonnet"),
                                    tokens =
                                        TokenCounts(
                                            inputTokens = TokenCount(200),
                                            outputTokens = TokenCount(400),
                                            cacheReadTokens = TokenCount(20),
                                            cacheWriteTokens = TokenCount(40),
                                        ),
                                    costUsdCent = DollarCent(100),
                                    estimatedCO2 = Co2Gram(10.0),
                                ),
                            ).toNonEmptyListOrNull()!!,
                        ),
                    ),
                )
        }

        test("roll up by WEEK keeps days of different weeks apart, newest week first") {
            // june14 is a Sunday, june15 the following Monday
            val dayGroups = listOf(DatedModelUsages(june14, usages("sonnet")), DatedModelUsages(june15, usages("opus")))

            dayGroups.rollUp(Granularity.WEEK).map { it.date } shouldBe listOf(june15, Day(LocalDate.of(2026, 6, 8)))
        }

        test("roll up of no day groups is empty") {
            emptyList<DatedModelUsages>().rollUp(Granularity.WEEK) shouldBe emptyList()
        }

        test("sum by model merges the same model across days") {
            val dayGroups = listOf(DatedModelUsages(june14, usages("sonnet")), DatedModelUsages(june15, usages("sonnet")))

            val summed = dayGroups.sumByModel() as ModelUsages.HasUsage

            summed.entries.single().tokens.inputTokens shouldBe TokenCount(200)
            summed.entries.single().costUsdCent shouldBe DollarCent(100)
        }

        test("sum by model keeps different models apart") {
            val dayGroups = listOf(DatedModelUsages(june14, usages("sonnet")), DatedModelUsages(june15, usages("opus")))

            (dayGroups.sumByModel() as ModelUsages.HasUsage).entries.map { it.model }.toSet() shouldBe
                setOf(ModelName.from("sonnet"), ModelName.from("opus"))
        }

        test("sum by model of no day groups is NoUsage") {
            emptyList<DatedModelUsages>().sumByModel() shouldBe ModelUsages.NoUsage
        }
    })
