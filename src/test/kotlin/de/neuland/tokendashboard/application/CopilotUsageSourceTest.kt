package de.neuland.tokendashboard.application

import arrow.core.nonEmptyListOf
import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2Factor
import de.neuland.tokendashboard.domain.model.Co2FactorVersion
import de.neuland.tokendashboard.domain.model.Co2FactorVersions
import de.neuland.tokendashboard.domain.model.Co2Factors
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.CopilotRawModelUsage
import de.neuland.tokendashboard.domain.model.DatedModelUsages
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.ModelUsages
import de.neuland.tokendashboard.domain.model.NanoAiu
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class CopilotUsageSourceTest :
    FunSpec({
        test("daily usage valuates the daily tokens of the repository with the stored co2 factor history") {
            val day = Day(LocalDate.of(2026, 6, 14))
            val range = DayRange.of(day, day)
            val model = ModelName.from("gpt-5.4-mini")
            val tokens = TokenCounts(TokenCount(1_000_000), TokenCount.ZERO, TokenCount.ZERO, TokenCount.ZERO)
            val usageRepository = mockk<CopilotUsageRepositoryPort>()
            val co2FactorRepository = mockk<Co2FactorRepositoryPort>()
            every { usageRepository.copilotDailyUsage(range) } returns
                mapOf(day to listOf(CopilotRawModelUsage(model, tokens, NanoAiu(0))))
            every { co2FactorRepository.co2FactorHistory() } returns
                Co2FactorVersions(
                    listOf(
                        Co2FactorVersion(
                            model.family(),
                            day,
                            Co2Factors(Co2Factor(100.0), Co2Factor.ZERO, Co2Factor.ZERO, Co2Factor.ZERO),
                        ),
                    ),
                )

            CopilotUsageSource(usageRepository, co2FactorRepository).dailyUsage(range) shouldBe
                listOf(
                    DatedModelUsages(
                        day,
                        ModelUsages.HasUsage(nonEmptyListOf(ModelUsage(model, tokens, DollarCent(0), Co2Gram(100.0)))),
                    ),
                )
        }
    })
