package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
import de.neuland.tokendashboard.domain.model.CentPerMillionToken
import de.neuland.tokendashboard.domain.model.ClaudePriceVersion
import de.neuland.tokendashboard.domain.model.ClaudePriceVersions
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class PriceHistoryQueryServiceTest :
    FunSpec({

        fun sonnetPrices(input: Long) =
            ClaudePrices(
                inputCentPerMillion = CentPerMillionToken(input),
                outputCentPerMillion = CentPerMillionToken(input * 5),
                cacheWrite5mCentPerMillion = CentPerMillionToken(input * 5 / 4),
                cacheWrite1hCentPerMillion = CentPerMillionToken(input * 2),
                cacheReadCentPerMillion = CentPerMillionToken(input / 10),
            )

        test("returns every price version, not only the currently valid one") {
            val priceRepository = mockk<ClaudePriceRepositoryPort>()
            val history =
                ClaudePriceVersions(
                    listOf(
                        ClaudePriceVersion(ModelFamily("sonnet"), Day(LocalDate.of(2026, 1, 1)), sonnetPrices(300)),
                        ClaudePriceVersion(ModelFamily("sonnet"), Day(LocalDate.of(2026, 8, 12)), sonnetPrices(200)),
                    ),
                )
            every { priceRepository.priceHistory() } returns history

            val result = PriceHistoryQueryService(priceRepository).queryPriceHistory()

            result shouldBe history
        }
    })
