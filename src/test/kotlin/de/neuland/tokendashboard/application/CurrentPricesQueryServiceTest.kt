package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
import de.neuland.tokendashboard.domain.model.CentPerMillionToken
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily
import de.neuland.tokendashboard.testsupport.fixedTimeFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class CurrentPricesQueryServiceTest :
    FunSpec({

        test("queries current prices for today") {
            val today = Day(LocalDate.of(2026, 6, 15))
            val priceRepository = mockk<ClaudePriceRepositoryPort>()
            val timeFactory = fixedTimeFactory(today.value)
            val prices =
                mapOf(
                    ModelFamily("sonnet") to
                        ClaudePrices(
                            inputCentPerMillion = CentPerMillionToken(300L),
                            outputCentPerMillion = CentPerMillionToken(1500L),
                            cacheWrite5mCentPerMillion = CentPerMillionToken(375L),
                            cacheWrite1hCentPerMillion = CentPerMillionToken(600L),
                            cacheReadCentPerMillion = CentPerMillionToken(30L),
                        ),
                )
            every { priceRepository.currentPricesPerFamily(today) } returns prices

            val result = CurrentPricesQueryService(priceRepository, timeFactory).queryCurrentPrices()

            result shouldBe prices
        }
    })
