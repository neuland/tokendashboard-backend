package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.Co2Factor
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class Co2FactorRepositoryAdapterTest :
    FunSpec({

        val jdbi = mockk<Co2FactorRepository>()
        val adapter = Co2FactorRepositoryAdapter(jdbi)

        val date = Day(LocalDate.of(2026, 6, 14))

        test("retrieves values by family") {
            every { jdbi.findLatestRateFor("opus", date.value) } returns
                Co2FactorRow(
                    model = "opus",
                    inputFactor = 1.0,
                    outputFactor = 2.0,
                    cacheReadFactor = 3.0,
                    cacheWriteFactor = 4.0,
                )

            val result = adapter.co2FactorsFor(ModelFamily("opus"), date)

            verify { jdbi.findLatestRateFor("opus", date.value) }
            result shouldBe
                de.neuland.tokendashboard.domain.model.Co2Factors(
                    inputFactor = Co2Factor(1.0),
                    outputFactor = Co2Factor(2.0),
                    cacheReadFactor = Co2Factor(3.0),
                    cacheWriteFactor = Co2Factor(4.0),
                )
        }

        test("returns null if no value for family") {
            val unknownFamily = "some-unknown-model-family"
            every { jdbi.findLatestRateFor(unknownFamily, date.value) } returns null

            val result = adapter.co2FactorsFor(ModelFamily(unknownFamily), date)

            verify { jdbi.findLatestRateFor(unknownFamily, date.value) }
            result.shouldBeNull()
        }
    })
