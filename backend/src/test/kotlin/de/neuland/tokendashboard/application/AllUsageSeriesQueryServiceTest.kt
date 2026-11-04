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
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class AllUsageSeriesQueryServiceTest :
    FunSpec({
        val day14 = Day(LocalDate.of(2026, 6, 14))
        val day15 = Day(LocalDate.of(2026, 6, 15))
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

        fun usages(vararg models: String) = ModelUsages.HasUsage(models.map { modelUsage(it) }.toNonEmptyListOrNull()!!)

        fun service(
            claudeUsageSource: ProviderUsageSource = mockk(),
            copilotSource: ProviderUsageSource = mockk(),
            openCodeSource: ProviderUsageSource = mockk(),
            userRepository: UserRepositoryPort = mockk(),
        ) = AllUsageSeriesQueryService(ProviderUsageSources(claudeUsageSource, copilotSource, openCodeSource), userRepository)

        test("produces a bucket for a day on which only Claude has usage") {
            val claudeUsageSource = mockk<ProviderUsageSource>()
            val copilotRepository = mockk<ProviderUsageSource>()
            val openCodeRepository = mockk<ProviderUsageSource>()
            val userRepository = mockk<UserRepositoryPort>()

            every { claudeUsageSource.dailyUsage(any()) } returns
                listOf(DatedModelUsages(day14, usages("claude-sonnet-4")))
            every { copilotRepository.dailyUsage(any()) } returns emptyList()
            every { openCodeRepository.dailyUsage(any()) } returns emptyList()
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns emptyMap()

            val result =
                service(claudeUsageSource, copilotRepository, openCodeRepository, userRepository)
                    .queryAllUsageSeries(DayRange.of(day14, day15), granularity)

            result.buckets.map { it.date } shouldBe listOf(day14)
            result.buckets
                .single()
                .providerBuckets.keys shouldBe setOf(CLAUDE)
        }

        test("produces a bucket for a day on which only OpenCode has usage") {
            val claudeUsageSource = mockk<ProviderUsageSource>()
            val copilotRepository = mockk<ProviderUsageSource>()
            val openCodeRepository = mockk<ProviderUsageSource>()
            val userRepository = mockk<UserRepositoryPort>()

            every { claudeUsageSource.dailyUsage(any()) } returns emptyList()
            every { copilotRepository.dailyUsage(any()) } returns emptyList()
            every { openCodeRepository.dailyUsage(any()) } returns
                listOf(DatedModelUsages(day14, usages("claude-sonnet-4")))
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns emptyMap()

            val result =
                service(claudeUsageSource, copilotRepository, openCodeRepository, userRepository)
                    .queryAllUsageSeries(DayRange.of(day14, day15), granularity)

            result.buckets.map { it.date } shouldBe listOf(day14)
            result.buckets
                .single()
                .providerBuckets.keys shouldBe setOf(OPENCODE)
        }

        test("produces one bucket per date across all three providers") {
            val claudeUsageSource = mockk<ProviderUsageSource>()
            val copilotRepository = mockk<ProviderUsageSource>()
            val openCodeRepository = mockk<ProviderUsageSource>()
            val userRepository = mockk<UserRepositoryPort>()

            every { claudeUsageSource.dailyUsage(any()) } returns
                listOf(DatedModelUsages(day14, usages("claude-sonnet-4")))
            every { copilotRepository.dailyUsage(any()) } returns
                listOf(DatedModelUsages(day14, usages("gpt-4")))
            every { openCodeRepository.dailyUsage(any()) } returns
                listOf(DatedModelUsages(day15, usages("claude-sonnet-4")))
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns emptyMap()

            val result =
                service(claudeUsageSource, copilotRepository, openCodeRepository, userRepository)
                    .queryAllUsageSeries(DayRange.of(day14, day15), granularity)

            result.buckets.map { it.date } shouldBe listOf(day15, day14)
            result.buckets
                .first { it.date == day15 }
                .providerBuckets.keys shouldBe setOf(OPENCODE)
        }
    })
