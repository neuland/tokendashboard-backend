package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.Co2Factor
import de.neuland.tokendashboard.domain.model.Co2FactorVersion
import de.neuland.tokendashboard.domain.model.Co2FactorVersions
import de.neuland.tokendashboard.domain.model.Co2Factors
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class Co2FactorRepositoryAdapterTest :
    FunSpec({

        val jdbi = mockk<Co2FactorRepository>()
        val adapter = Co2FactorRepositoryAdapter(jdbi)

        test("maps every factor version with its validity date") {
            every { jdbi.findAllFactorVersions() } returns
                listOf(
                    Co2FactorVersionRow(
                        model = "opus",
                        validFrom = LocalDate.of(2026, 1, 1),
                        inputFactor = 70.0,
                        outputFactor = 1400.0,
                        cacheReadFactor = 0.7,
                        cacheWriteFactor = 87.0,
                    ),
                )

            val result = adapter.co2FactorHistory()

            result shouldBe
                Co2FactorVersions(
                    listOf(
                        Co2FactorVersion(
                            family = ModelFamily("opus"),
                            validFrom = Day(LocalDate.of(2026, 1, 1)),
                            factors =
                                Co2Factors(
                                    inputFactor = Co2Factor(70.0),
                                    outputFactor = Co2Factor(1400.0),
                                    cacheReadFactor = Co2Factor(0.7),
                                    cacheWriteFactor = Co2Factor(87.0),
                                ),
                        ),
                    ),
                )
        }
    })
