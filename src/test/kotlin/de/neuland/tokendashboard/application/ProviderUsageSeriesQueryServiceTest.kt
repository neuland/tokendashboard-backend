package de.neuland.tokendashboard.application

import arrow.core.toNonEmptyListOrNull
import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.ModelUsagesAtDate
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageBucket
import de.neuland.tokendashboard.domain.model.UsageResult
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
            claudeUsageRepository: ClaudeUsageRepositoryPort = mockk(),
            copilotUsageRepository: CopilotUsageRepositoryPort = mockk(),
            openCodeUsageRepository: OpenCodeUsageRepositoryPort = mockk(),
            userRepository: UserRepositoryPort = mockk(),
        ) = ProviderUsageSeriesQueryService(claudeUsageRepository, copilotUsageRepository, openCodeUsageRepository, userRepository)

        test("delegates to the Claude repository when provider is CLAUDE") {
            val usageRepository = mockk<ClaudeUsageRepositoryPort>()
            val userRepository = mockk<UserRepositoryPort>()
            val rangeSlot = slot<DayRange>()
            val granularitySlot = slot<Granularity>()
            val pluginRangeSlot = slot<DayRange>()

            every {
                usageRepository.claudeUsageSeries(capture(rangeSlot), capture(granularitySlot))
            } returns emptyList()
            every {
                userRepository.pluginInstallationsByDate(capture(pluginRangeSlot), any(), eq(CLAUDE))
            } returns emptyMap()

            service(claudeUsageRepository = usageRepository, userRepository = userRepository)
                .querySeries(CLAUDE, DayRange.of(from, to), granularity)

            rangeSlot.captured.from shouldBe from
            rangeSlot.captured.to shouldBe to
            granularitySlot.captured shouldBe granularity
            pluginRangeSlot.captured.from shouldBe from
            pluginRangeSlot.captured.to shouldBe to
        }

        test("delegates to the Copilot repository when provider is COPILOT") {
            val usageRepository = mockk<CopilotUsageRepositoryPort>()
            val userRepository = mockk<UserRepositoryPort>()

            every { usageRepository.copilotUsageSeries(any(), any()) } returns emptyList()
            every { userRepository.pluginInstallationsByDate(any(), any(), eq(COPILOT)) } returns emptyMap()

            val result =
                service(copilotUsageRepository = usageRepository, userRepository = userRepository)
                    .querySeries(COPILOT, DayRange.of(from, to), granularity)

            result.buckets.shouldBeEmpty()
        }

        test("delegates to the OpenCode repository when provider is OPENCODE") {
            val usageRepository = mockk<OpenCodeUsageRepositoryPort>()
            val userRepository = mockk<UserRepositoryPort>()

            every { usageRepository.openCodeUsageSeries(any(), any()) } returns emptyList()
            every { userRepository.pluginInstallationsByDate(any(), any(), eq(OPENCODE)) } returns emptyMap()

            val result =
                service(openCodeUsageRepository = usageRepository, userRepository = userRepository)
                    .querySeries(OPENCODE, DayRange.of(from, to), granularity)

            result.buckets.shouldBeEmpty()
        }

        test("returns empty buckets list when there are no day-groups") {
            val usageRepository = mockk<ClaudeUsageRepositoryPort>()
            val userRepository = mockk<UserRepositoryPort>()
            every { usageRepository.claudeUsageSeries(any(), any()) } returns emptyList()
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns emptyMap()

            val result =
                service(claudeUsageRepository = usageRepository, userRepository = userRepository)
                    .querySeries(CLAUDE, DayRange.of(from, to), granularity)

            result.buckets.shouldBeEmpty()
        }

        test("produces one bucket per day-group with overallTotal matching sum of modelUsages") {
            val usageRepository = mockk<ClaudeUsageRepositoryPort>()
            val userRepository = mockk<UserRepositoryPort>()

            val day14Usages = UsageResult.HasUsage(listOf(modelUsage("sonnet"), modelUsage("haiku")).toNonEmptyListOrNull()!!)
            val day15Usages = UsageResult.HasUsage(listOf(modelUsage("opus")).toNonEmptyListOrNull()!!)

            every { usageRepository.claudeUsageSeries(any(), any()) } returns
                listOf(
                    ModelUsagesAtDate(from, day14Usages),
                    ModelUsagesAtDate(to, day15Usages),
                )
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns
                mapOf(from to UserCount(3), to to UserCount(5))

            val result =
                service(claudeUsageRepository = usageRepository, userRepository = userRepository)
                    .querySeries(CLAUDE, DayRange.of(from, to), granularity)

            result.buckets shouldBe
                listOf(
                    UsageBucket(
                        date = from,
                        modelUsages = day14Usages,
                        pluginInstallations = UserCount(3),
                    ),
                    UsageBucket(
                        date = to,
                        modelUsages = day15Usages,
                        pluginInstallations = UserCount(5),
                    ),
                )
        }

        test("pluginInstallations are matched by date, not position") {
            val usageRepository = mockk<ClaudeUsageRepositoryPort>()
            val userRepository = mockk<UserRepositoryPort>()

            val day14Usages = UsageResult.HasUsage(listOf(modelUsage("sonnet")).toNonEmptyListOrNull()!!)
            val day15Usages = UsageResult.HasUsage(listOf(modelUsage("opus")).toNonEmptyListOrNull()!!)

            every { usageRepository.claudeUsageSeries(any(), any()) } returns
                listOf(
                    ModelUsagesAtDate(from, day14Usages),
                    ModelUsagesAtDate(to, day15Usages),
                )
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns
                mapOf(to to UserCount(5), from to UserCount(3))

            val result =
                service(claudeUsageRepository = usageRepository, userRepository = userRepository)
                    .querySeries(CLAUDE, DayRange.of(from, to), granularity)

            result.buckets.first { it.date == from }.pluginInstallations shouldBe UserCount(3)
            result.buckets.first { it.date == to }.pluginInstallations shouldBe UserCount(5)
        }

        test("falls back to UserCount(0) when a day-group's date is missing from the plugin-installations map") {
            val usageRepository = mockk<ClaudeUsageRepositoryPort>()
            val userRepository = mockk<UserRepositoryPort>()

            val day14Usages = UsageResult.HasUsage(listOf(modelUsage("sonnet")).toNonEmptyListOrNull()!!)

            every { usageRepository.claudeUsageSeries(any(), any()) } returns
                listOf(ModelUsagesAtDate(from, day14Usages))
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns emptyMap()

            val result =
                service(claudeUsageRepository = usageRepository, userRepository = userRepository)
                    .querySeries(CLAUDE, DayRange.of(from, to), granularity)

            result.buckets.single().pluginInstallations shouldBe UserCount(0)
        }
    })
