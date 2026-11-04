package de.neuland.tokendashboard.adapter.outgoing.repository

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

class PriceRepositoryAdapterTest :
    FunSpec({

        val jdbi = mockk<ClaudePriceRepository>()
        val adapter = ClaudePriceRepositoryAdapter(jdbi)

        val date = Day(LocalDate.of(2026, 6, 14))

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

        test("maps every price version with its validity date") {
            every { jdbi.findAllPriceVersions() } returns
                listOf(
                    PriceVersionRow(
                        model = "sonnet",
                        validFrom = LocalDate.of(2026, 1, 1),
                        inputCentPerMillion = 300L,
                        outputCentPerMillion = 1500L,
                        cacheWrite5mCentPerMillion = 375L,
                        cacheWrite1hCentPerMillion = 600L,
                        cacheReadCentPerMillion = 30L,
                    ),
                    PriceVersionRow(
                        model = "sonnet",
                        validFrom = LocalDate.of(2026, 8, 12),
                        inputCentPerMillion = 200L,
                        outputCentPerMillion = 1000L,
                        cacheWrite5mCentPerMillion = 250L,
                        cacheWrite1hCentPerMillion = 400L,
                        cacheReadCentPerMillion = 20L,
                    ),
                )

            val result = adapter.priceHistory()

            result shouldBe
                ClaudePriceVersions(
                    listOf(
                        ClaudePriceVersion(
                            family = ModelFamily("sonnet"),
                            validFrom = Day(LocalDate.of(2026, 1, 1)),
                            prices =
                                ClaudePrices(
                                    inputCentPerMillion = CentPerMillionToken(300L),
                                    outputCentPerMillion = CentPerMillionToken(1500L),
                                    cacheWrite5mCentPerMillion = CentPerMillionToken(375L),
                                    cacheWrite1hCentPerMillion = CentPerMillionToken(600L),
                                    cacheReadCentPerMillion = CentPerMillionToken(30L),
                                ),
                        ),
                        ClaudePriceVersion(
                            family = ModelFamily("sonnet"),
                            validFrom = Day(LocalDate.of(2026, 8, 12)),
                            prices =
                                ClaudePrices(
                                    inputCentPerMillion = CentPerMillionToken(200L),
                                    outputCentPerMillion = CentPerMillionToken(1000L),
                                    cacheWrite5mCentPerMillion = CentPerMillionToken(250L),
                                    cacheWrite1hCentPerMillion = CentPerMillionToken(400L),
                                    cacheReadCentPerMillion = CentPerMillionToken(20L),
                                ),
                        ),
                    ),
                )
        }
    })
