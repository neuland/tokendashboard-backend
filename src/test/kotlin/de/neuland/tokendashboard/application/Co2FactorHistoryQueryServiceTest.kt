package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
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

class Co2FactorHistoryQueryServiceTest :
    FunSpec({

        test("returns every CO2 factor version of every family") {
            val co2FactorRepository = mockk<Co2FactorRepositoryPort>()
            val history =
                Co2FactorVersions(
                    listOf(
                        Co2FactorVersion(
                            ModelFamily("opus"),
                            Day(LocalDate.of(2026, 1, 1)),
                            Co2Factors(Co2Factor(70.0), Co2Factor(1400.0), Co2Factor(0.7), Co2Factor(87.0)),
                        ),
                    ),
                )
            every { co2FactorRepository.co2FactorHistory() } returns history

            val result = Co2FactorHistoryQueryService(co2FactorRepository).queryCo2FactorHistory()

            result shouldBe history
        }
    })
