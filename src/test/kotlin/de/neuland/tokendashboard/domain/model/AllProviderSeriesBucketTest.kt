package de.neuland.tokendashboard.domain.model

import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class AllProviderSeriesBucketTest :
    FunSpec({
        val day14 = Day(LocalDate.of(2026, 6, 14))
        val day15 = Day(LocalDate.of(2026, 6, 15))

        fun bucket(date: Day) =
            UsageBucket(
                date = date,
                modelUsages = ModelUsages.NoUsage,
                pluginInstallations = UserCount(0),
            )

        test("byDate groups the providers' buckets by their own date, newest first") {
            val buckets =
                AllProviderSeriesBucket.byDate(
                    mapOf(
                        CLAUDE to listOf(bucket(day14), bucket(day15)),
                        COPILOT to listOf(bucket(day15)),
                        OPENCODE to emptyList(),
                    ),
                )

            buckets.map { it.date } shouldBe listOf(day15, day14)
            buckets.map { it.providerBuckets.keys } shouldBe listOf(setOf(CLAUDE, COPILOT), setOf(CLAUDE))
        }

        test("byDate gives every provider bucket the date of its group") {
            val buckets = AllProviderSeriesBucket.byDate(mapOf(CLAUDE to listOf(bucket(day14)), COPILOT to listOf(bucket(day15))))

            buckets.forEach { allBucket -> allBucket.providerBuckets.values.map { it.date }.toSet() shouldBe setOf(allBucket.date) }
        }

        test("byDate without any bucket returns no buckets") {
            AllProviderSeriesBucket.byDate(mapOf(CLAUDE to emptyList())).shouldBeEmpty()
        }
    })
