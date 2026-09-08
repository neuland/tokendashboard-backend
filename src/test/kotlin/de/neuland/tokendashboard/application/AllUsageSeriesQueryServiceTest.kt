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
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageResult
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

        fun usages(vararg models: String) = UsageResult.HasUsage(models.map { modelUsage(it) }.toNonEmptyListOrNull()!!)

        fun service(
            claudeRepository: ClaudeUsageRepositoryPort = mockk(),
            copilotRepository: CopilotUsageRepositoryPort = mockk(),
            openCodeRepository: OpenCodeUsageRepositoryPort = mockk(),
            userRepository: UserRepositoryPort = mockk(),
        ) = AllUsageSeriesQueryService(claudeRepository, copilotRepository, openCodeRepository, userRepository)

        test("produces a bucket for a day on which only Claude has usage") {
            val claudeRepository = mockk<ClaudeUsageRepositoryPort>()
            val copilotRepository = mockk<CopilotUsageRepositoryPort>()
            val openCodeRepository = mockk<OpenCodeUsageRepositoryPort>()
            val userRepository = mockk<UserRepositoryPort>()

            every { claudeRepository.claudeUsageSeries(any(), any()) } returns
                listOf(ModelUsagesAtDate(day14, usages("claude-sonnet-4")))
            every { copilotRepository.copilotUsageSeries(any(), any()) } returns emptyList()
            every { openCodeRepository.openCodeUsageSeries(any(), any()) } returns emptyList()
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns emptyMap()

            val result =
                service(claudeRepository, copilotRepository, openCodeRepository, userRepository)
                    .queryAllUsageSeries(DayRange.of(day14, day15), granularity)

            result.buckets.map { it.date } shouldBe listOf(day14)
            result.buckets
                .single()
                .providerBuckets.keys shouldBe setOf(CLAUDE)
        }

        test("produces a bucket for a day on which only OpenCode has usage") {
            val claudeRepository = mockk<ClaudeUsageRepositoryPort>()
            val copilotRepository = mockk<CopilotUsageRepositoryPort>()
            val openCodeRepository = mockk<OpenCodeUsageRepositoryPort>()
            val userRepository = mockk<UserRepositoryPort>()

            every { claudeRepository.claudeUsageSeries(any(), any()) } returns emptyList()
            every { copilotRepository.copilotUsageSeries(any(), any()) } returns emptyList()
            every { openCodeRepository.openCodeUsageSeries(any(), any()) } returns
                listOf(ModelUsagesAtDate(day14, usages("claude-sonnet-4")))
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns emptyMap()

            val result =
                service(claudeRepository, copilotRepository, openCodeRepository, userRepository)
                    .queryAllUsageSeries(DayRange.of(day14, day15), granularity)

            result.buckets.map { it.date } shouldBe listOf(day14)
            result.buckets
                .single()
                .providerBuckets.keys shouldBe setOf(OPENCODE)
        }

        test("produces one bucket per date across all three providers") {
            val claudeRepository = mockk<ClaudeUsageRepositoryPort>()
            val copilotRepository = mockk<CopilotUsageRepositoryPort>()
            val openCodeRepository = mockk<OpenCodeUsageRepositoryPort>()
            val userRepository = mockk<UserRepositoryPort>()

            every { claudeRepository.claudeUsageSeries(any(), any()) } returns
                listOf(ModelUsagesAtDate(day14, usages("claude-sonnet-4")))
            every { copilotRepository.copilotUsageSeries(any(), any()) } returns
                listOf(ModelUsagesAtDate(day14, usages("gpt-4")))
            every { openCodeRepository.openCodeUsageSeries(any(), any()) } returns
                listOf(ModelUsagesAtDate(day15, usages("claude-sonnet-4")))
            every { userRepository.pluginInstallationsByDate(any(), any(), any()) } returns emptyMap()

            val result =
                service(claudeRepository, copilotRepository, openCodeRepository, userRepository)
                    .queryAllUsageSeries(DayRange.of(day14, day15), granularity)

            result.buckets.map { it.date } shouldBe listOf(day15, day14)
            result.buckets
                .first { it.date == day15 }
                .providerBuckets.keys shouldBe setOf(OPENCODE)
        }
    })
