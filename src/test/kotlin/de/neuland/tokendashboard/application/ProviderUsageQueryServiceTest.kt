package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.ProviderUsageDetail
import de.neuland.tokendashboard.domain.model.UsageResult
import de.neuland.tokendashboard.domain.model.UserCount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class ProviderUsageQueryServiceTest :
    FunSpec({
        val from = Day(LocalDate.of(2026, 6, 1))
        val to = Day(LocalDate.of(2026, 6, 15))
        val range = DayRange.of(from, to)

        val claudeUsage = ProviderUsageDetail(CLAUDE, range, UsageResult.NoUsage)
        val copilotUsage = ProviderUsageDetail(COPILOT, range, UsageResult.NoUsage)
        val openCodeUsage = ProviderUsageDetail(OPENCODE, range, UsageResult.NoUsage)

        val claudeRepository = mockk<ClaudeUsageRepositoryPort>()
        val copilotRepository = mockk<CopilotUsageRepositoryPort>()
        val openCodeRepository = mockk<OpenCodeUsageRepositoryPort>()
        val userRepository = mockk<UserRepositoryPort>()

        every { claudeRepository.claudeUsageByDateRange(any()) } returns claudeUsage
        every { copilotRepository.copilotUsageByDateRange(any()) } returns copilotUsage
        every { openCodeRepository.openCodeUsageByDateRange(any()) } returns openCodeUsage
        every { userRepository.activeUsersLast4Weeks(any()) } returns UserCount(42)

        val service = ProviderUsageQueryService(claudeRepository, copilotRepository, openCodeRepository, userRepository)

        test("query usage delegates to claude repository") {
            val result = service.queryUsage(CLAUDE, DayRange.of(from, to))
            result.usage shouldBe claudeUsage
            result.activeUsersLast4Weeks shouldBe UserCount(42)
        }

        test("query usage delegates to copilot repository") {
            val result = service.queryUsage(COPILOT, DayRange.of(from, to))
            result.usage shouldBe copilotUsage
        }

        test("query usage delegates to open code repository") {
            val result = service.queryUsage(OPENCODE, DayRange.of(from, to))
            result.usage shouldBe openCodeUsage
        }
    })
