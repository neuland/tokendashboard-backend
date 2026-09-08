package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.CentPerMillionToken
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class PriceRepositoryAdapterTest :
    FunSpec({

        val jdbi = mockk<ClaudePriceRepository>()
        val adapter = ClaudePriceRepositoryAdapter(jdbi)

        val date = Day(LocalDate.of(2026, 6, 14))

        test("retrieves values by model family") {
            every { jdbi.findLatestPriceFor("opus", date.value) } returns
                PriceRow(
                    model = "opus",
                    inputCentPerMillion = 100L,
                    outputCentPerMillion = 200L,
                    cacheWrite5mCentPerMillion = 300L,
                    cacheWrite1hCentPerMillion = 1000L,
                    cacheReadCentPerMillion = 400L,
                )

            val result = adapter.priceFor(ModelFamily("opus"), date)

            verify { jdbi.findLatestPriceFor("opus", date.value) }
            result shouldBe
                ClaudePrices(
                    inputCentPerMillion = CentPerMillionToken(100L),
                    outputCentPerMillion = CentPerMillionToken(200L),
                    cacheWrite5mCentPerMillion = CentPerMillionToken(300L),
                    cacheWrite1hCentPerMillion = CentPerMillionToken(1000L),
                    cacheReadCentPerMillion = CentPerMillionToken(400L),
                )
        }

        test("maps all current prices keyed by model family") {
            every { jdbi.findAllLatestPrices(date.value) } returns
                listOf(
                    PriceRow(
                        model = "opus",
                        inputCentPerMillion = 500L,
                        outputCentPerMillion = 2500L,
                        cacheWrite5mCentPerMillion = 625L,
                        cacheWrite1hCentPerMillion = 1000L,
                        cacheReadCentPerMillion = 50L,
                    ),
                    PriceRow(
                        model = "haiku",
                        inputCentPerMillion = 100L,
                        outputCentPerMillion = 500L,
                        cacheWrite5mCentPerMillion = 125L,
                        cacheWrite1hCentPerMillion = 200L,
                        cacheReadCentPerMillion = 10L,
                    ),
                )

            val result = adapter.currentPricesPerFamily(date)

            result shouldBe
                mapOf(
                    ModelFamily("opus") to
                        ClaudePrices(
                            inputCentPerMillion = CentPerMillionToken(500L),
                            outputCentPerMillion = CentPerMillionToken(2500L),
                            cacheWrite5mCentPerMillion = CentPerMillionToken(625L),
                            cacheWrite1hCentPerMillion = CentPerMillionToken(1000L),
                            cacheReadCentPerMillion = CentPerMillionToken(50L),
                        ),
                    ModelFamily("haiku") to
                        ClaudePrices(
                            inputCentPerMillion = CentPerMillionToken(100L),
                            outputCentPerMillion = CentPerMillionToken(500L),
                            cacheWrite5mCentPerMillion = CentPerMillionToken(125L),
                            cacheWrite1hCentPerMillion = CentPerMillionToken(200L),
                            cacheReadCentPerMillion = CentPerMillionToken(10L),
                        ),
                )
        }

        test("returns null if no value for family") {
            val unknownFamily = "some-unknown-model-family"
            every { jdbi.findLatestPriceFor(unknownFamily, date.value) } returns null

            val result = adapter.priceFor(ModelFamily(unknownFamily), date)

            verify { jdbi.findLatestPriceFor(unknownFamily, date.value) }
            result.shouldBeNull()
        }
    })
