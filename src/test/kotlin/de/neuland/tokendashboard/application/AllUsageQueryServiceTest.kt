package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.ProviderUsage
import de.neuland.tokendashboard.domain.model.TokenCount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class AllUsageQueryServiceTest :
    FunSpec({
        val from = Day(LocalDate.of(2026, 6, 1))
        val to = Day(LocalDate.of(2026, 6, 15))

        val claudeUsage = ProviderUsage(CLAUDE, TokenCount(1), Co2Gram(1.0), DollarCent(1))
        val copilotUsage = ProviderUsage(COPILOT, TokenCount(2), Co2Gram(2.0), DollarCent(2))
        val openCodeUsage = ProviderUsage(OPENCODE, TokenCount(3), Co2Gram(3.0), DollarCent(3))

        val claudeRepository = mockk<ClaudeUsageRepositoryPort>()
        val copilotRepository = mockk<CopilotUsageRepositoryPort>()
        val openCodeRepository = mockk<OpenCodeUsageRepositoryPort>()

        every { claudeRepository.claudeProviderUsage(any()) } returns claudeUsage
        every { copilotRepository.copilotProviderUsage(any()) } returns copilotUsage
        every { openCodeRepository.openCodeProviderUsage(any()) } returns openCodeUsage

        val service = AllUsageQueryService(claudeRepository, copilotRepository, openCodeRepository)

        test("query all usage maps each provider to its own usage without mis-keying") {
            val result = service.queryAllUsage(DayRange.of(from, to))

            result.providerUsages.values.first { it.provider == CLAUDE } shouldBe claudeUsage
            result.providerUsages.values.first { it.provider == COPILOT } shouldBe copilotUsage
            result.providerUsages.values.first { it.provider == OPENCODE } shouldBe openCodeUsage
        }

        test("query all usage passes through from and to") {
            val result = service.queryAllUsage(DayRange.of(from, to))

            result.range.from shouldBe from
            result.range.to shouldBe to
        }
    })
