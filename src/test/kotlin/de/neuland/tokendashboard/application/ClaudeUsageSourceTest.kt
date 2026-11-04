package de.neuland.tokendashboard.application

import arrow.core.nonEmptyListOf
import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.domain.model.CentPerMillionToken
import de.neuland.tokendashboard.domain.model.ClaudePriceVersion
import de.neuland.tokendashboard.domain.model.ClaudePriceVersions
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.ClaudeRawModelUsage
import de.neuland.tokendashboard.domain.model.ClaudeTokenCounts
import de.neuland.tokendashboard.domain.model.Co2Factor
import de.neuland.tokendashboard.domain.model.Co2FactorVersion
import de.neuland.tokendashboard.domain.model.Co2FactorVersions
import de.neuland.tokendashboard.domain.model.Co2Factors
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.DatedModelUsages
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.ModelFamily
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.ModelUsages
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class ClaudeUsageSourceTest :
    FunSpec({
        val day14 = Day(LocalDate.of(2026, 6, 14))
        val range = DayRange.of(day14, Day(LocalDate.of(2026, 6, 15)))
        val sonnet = ModelName.from("claude-sonnet-4-5")
        val zero = TokenCount.ZERO

        val usageRepository = mockk<ClaudeUsageRepositoryPort>()
        val priceRepository = mockk<ClaudePriceRepositoryPort>()
        val co2FactorRepository = mockk<Co2FactorRepositoryPort>()
        val source = ClaudeUsageSource(usageRepository, priceRepository, co2FactorRepository)

        every { priceRepository.priceHistory() } returns
            ClaudePriceVersions(
                listOf(
                    ClaudePriceVersion(
                        ModelFamily("sonnet"),
                        day14,
                        ClaudePrices(
                            CentPerMillionToken(300),
                            CentPerMillionToken.ZERO,
                            CentPerMillionToken.ZERO,
                            CentPerMillionToken.ZERO,
                            CentPerMillionToken.ZERO,
                        ),
                    ),
                ),
            )
        every { co2FactorRepository.co2FactorHistory() } returns
            Co2FactorVersions(
                listOf(
                    Co2FactorVersion(
                        ModelFamily("sonnet"),
                        day14,
                        Co2Factors(Co2Factor(100.0), Co2Factor.ZERO, Co2Factor.ZERO, Co2Factor.ZERO),
                    ),
                ),
            )

        test("daily usage reads token aggregates for the given range") {
            every { usageRepository.claudeDailyUsage(any()) } returns emptyMap()

            source.dailyUsage(range).shouldBeEmpty()

            verify { usageRepository.claudeDailyUsage(range) }
        }

        test("daily usage valuates the tokens with the stored price and co2 factor history") {
            every { usageRepository.claudeDailyUsage(any()) } returns
                mapOf(
                    day14 to
                        listOf(
                            ClaudeRawModelUsage(
                                sonnet,
                                ClaudeTokenCounts(TokenCount(1_000_000), zero, zero, zero, zero, zero),
                            ),
                        ),
                )

            source.dailyUsage(range) shouldBe
                listOf(
                    DatedModelUsages(
                        day14,
                        ModelUsages.HasUsage(
                            nonEmptyListOf(
                                ModelUsage(sonnet, TokenCounts(TokenCount(1_000_000), zero, zero, zero), DollarCent(300), Co2Gram(100.0)),
                            ),
                        ),
                    ),
                )
        }
    })
