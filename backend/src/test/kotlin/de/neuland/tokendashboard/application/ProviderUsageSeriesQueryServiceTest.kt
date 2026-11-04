package de.neuland.tokendashboard.application

import arrow.core.toNonEmptyListOrNull
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.DatedModelUsages
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.ModelUsages
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageBucket
import de.neuland.tokendashboard.domain.model.UserCount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate

class ProviderUsageSeriesQueryServiceTest :
    FunSpec({
        val from = Day(LocalDate.of(2026, 6, 14))
        val to = Day(LocalDate.of(2026, 6, 15))
        val granularity = Granularity.DAY

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

        fun service(
            claudeUsageSource: ProviderUsageSource = mockk(),
            copilotSource: ProviderUsageSource = mockk(),
            openCodeSource: ProviderUsageSource = mockk(),
            userRepository: UserRepositoryPort = mockk(),
        ) = ProviderUsageSeriesQueryService(ProviderUsageSources(claudeUsageSource, copilotSource, openCodeSource), userRepository)

        test("delegates to the Claude usage source when provider is CLAUDE") {
            val claudeUsageSource = mockk<ProviderUsageSource>()
            val userRepository = mockk<UserRepositoryPort>()
            val rangeSlot = slot<DayRange>()
            val granularitySlot = slot<Granularity>()
            val pluginRangeSlot = slot<DayRange>()

            every { claudeUsageSource.dailyUsage(capture(rangeSlot)) } returns emptyList()
            every {
                userRepository.pluginInstallationsByDate(capture(pluginRangeSlot), capture(granularitySlot), eq(CLAUDE))
            } returns emptyMap()

            service(claudeUsageSource = claudeUsageSource, userRepository = userRepository)
                .querySeries(CLAUDE, DayRange.of(from, to), granularity)

            rangeSlot.captured.from shouldBe from
            rangeSlot.captured.to shouldBe to
            granularitySlot.captured shouldBe granularity
            pluginRangeSlot.captured.from shouldBe from
            pluginRangeSlot.captured.to shouldBe to
        }

        test("delegates to the Copilot usage source when provider is COPILOT") {
            val usageRepository = mockk<ProviderUsageSource>()
            val userRepository = mockk<UserRepositoryPort>()

            every { usageRepository.dailyUsage(any()) } returns emptyList()
            every { userRepository.pluginInstallationsByDate(any(), any(), eq(COPILOT)) } returns emptyMap()

            val result =
                service(copilotSource = usageRepository, userRepository = userRepository)
                    .querySeries(COPILOT, DayRange.of(from, to), granularity)

            result.buckets.shouldBeEmpty()
        }

        test("delegates to the OpenCode usage source when provider is OPENCODE") {
            val usageRepository = mockk<ProviderUsageSource>()
            val userRepository = mockk<UserRepositoryPort>()

            every { usageRepository.dailyUsage(any()) } returns emptyList()
            every { userRepository.pluginInstallationsByDate(any(), any(), eq(OPENCODE)) } returns emptyMap()

            val result =
                service(openCodeSource = usageRepository, userRepository = userRepository)
                    .querySeries(OPENCODE, DayRange.of(from, to), granularity)

            result.buckets.shouldBeEmpty()
        }

        test("returns empty buckets list when there are no day-groups") {
            val claudeUsageSource = mockk<ProviderUsageSource>()
            val userRepository = mockk<UserRepositoryPort>()
            every { claudeUsageSource.dailyUsage(any()) } returns emptyList()
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns emptyMap()

            val result =
                service(claudeUsageSource = claudeUsageSource, userRepository = userRepository)
                    .querySeries(CLAUDE, DayRange.of(from, to), granularity)

            result.buckets.shouldBeEmpty()
        }

        test("produces one bucket per day-group with overallTotal matching sum of modelUsages") {
            val claudeUsageSource = mockk<ProviderUsageSource>()
            val userRepository = mockk<UserRepositoryPort>()

            val day14Usages = ModelUsages.HasUsage(listOf(modelUsage("sonnet"), modelUsage("haiku")).toNonEmptyListOrNull()!!)
            val day15Usages = ModelUsages.HasUsage(listOf(modelUsage("opus")).toNonEmptyListOrNull()!!)

            every { claudeUsageSource.dailyUsage(any()) } returns
                listOf(
                    DatedModelUsages(from, day14Usages),
                    DatedModelUsages(to, day15Usages),
                )
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns
                mapOf(from to UserCount(3), to to UserCount(5))

            val result =
                service(claudeUsageSource = claudeUsageSource, userRepository = userRepository)
                    .querySeries(CLAUDE, DayRange.of(from, to), granularity)

            result.buckets shouldBe
                listOf(
                    UsageBucket(
                        date = to,
                        modelUsages = day15Usages,
                        pluginInstallations = UserCount(5),
                    ),
                    UsageBucket(
                        date = from,
                        modelUsages = day14Usages,
                        pluginInstallations = UserCount(3),
                    ),
                )
        }

        test("pluginInstallations are matched by date, not position") {
            val claudeUsageSource = mockk<ProviderUsageSource>()
            val userRepository = mockk<UserRepositoryPort>()

            val day14Usages = ModelUsages.HasUsage(listOf(modelUsage("sonnet")).toNonEmptyListOrNull()!!)
            val day15Usages = ModelUsages.HasUsage(listOf(modelUsage("opus")).toNonEmptyListOrNull()!!)

            every { claudeUsageSource.dailyUsage(any()) } returns
                listOf(
                    DatedModelUsages(from, day14Usages),
                    DatedModelUsages(to, day15Usages),
                )
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns
                mapOf(to to UserCount(5), from to UserCount(3))

            val result =
                service(claudeUsageSource = claudeUsageSource, userRepository = userRepository)
                    .querySeries(CLAUDE, DayRange.of(from, to), granularity)

            result.buckets.first { it.date == from }.pluginInstallations shouldBe UserCount(3)
            result.buckets.first { it.date == to }.pluginInstallations shouldBe UserCount(5)
        }

        test("falls back to UserCount(0) when a day-group's date is missing from the plugin-installations map") {
            val claudeUsageSource = mockk<ProviderUsageSource>()
            val userRepository = mockk<UserRepositoryPort>()

            val day14Usages = ModelUsages.HasUsage(listOf(modelUsage("sonnet")).toNonEmptyListOrNull()!!)

            every { claudeUsageSource.dailyUsage(any()) } returns
                listOf(DatedModelUsages(from, day14Usages))
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns emptyMap()

            val result =
                service(claudeUsageSource = claudeUsageSource, userRepository = userRepository)
                    .querySeries(CLAUDE, DayRange.of(from, to), granularity)

            result.buckets.single().pluginInstallations shouldBe UserCount(0)
        }

        test("rolls Claude days up into weekly buckets keyed by monday for WEEK granularity") {
            val claudeUsageSource = mockk<ProviderUsageSource>()
            val userRepository = mockk<UserRepositoryPort>()
            val tuesday = Day(LocalDate.of(2026, 6, 16))

            every { claudeUsageSource.dailyUsage(any()) } returns
                listOf(
                    DatedModelUsages(tuesday, ModelUsages.HasUsage(listOf(modelUsage("sonnet")).toNonEmptyListOrNull()!!)),
                    DatedModelUsages(to, ModelUsages.HasUsage(listOf(modelUsage("sonnet")).toNonEmptyListOrNull()!!)),
                )
            every { userRepository.pluginInstallationsByDate(any(), eq(Granularity.WEEK), any()) } returns mapOf(to to UserCount(4))

            val result =
                service(claudeUsageSource = claudeUsageSource, userRepository = userRepository)
                    .querySeries(CLAUDE, DayRange.of(to, tuesday), Granularity.WEEK)

            val bucket = result.buckets.single()
            bucket.date shouldBe to
            bucket.overallTotal.tokens.inputTokens shouldBe TokenCount(200)
            bucket.pluginInstallations shouldBe UserCount(4)
        }
    })
