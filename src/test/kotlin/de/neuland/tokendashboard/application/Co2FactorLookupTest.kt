package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2Factor
import de.neuland.tokendashboard.domain.model.Co2Factors
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class Co2FactorLookupTest :
    FunSpec({
        val day = Day(LocalDate.of(2026, 6, 15))
        val sonnet = ModelFamily("sonnet")
        val haiku = ModelFamily("haiku")

        val factors =
            Co2Factors(
                inputFactor = Co2Factor(42.0),
                outputFactor = Co2Factor(840.0),
                cacheReadFactor = Co2Factor(0.42),
                cacheWriteFactor = Co2Factor(52.0),
            )

        test("returns the factors from the repository") {
            val repository = mockk<Co2FactorRepositoryPort>()
            every { repository.co2FactorsFor(any(), any()) } returns factors
            val lookup = Co2FactorLookup(repository)

            val result = lookup.factorFor(sonnet, day)

            result shouldBe factors
        }

        test("queries the repository only once per model family and day") {
            val repository = mockk<Co2FactorRepositoryPort>()
            every { repository.co2FactorsFor(any(), any()) } returns factors
            val lookup = Co2FactorLookup(repository)

            lookup.factorFor(sonnet, day)
            lookup.factorFor(sonnet, day)

            verify(exactly = 1) { repository.co2FactorsFor(any(), any()) }
        }

        test("caches a missing factor and does not query again") {
            val repository = mockk<Co2FactorRepositoryPort>()
            every { repository.co2FactorsFor(any(), any()) } returns null
            val lookup = Co2FactorLookup(repository)

            val first = lookup.factorFor(sonnet, day)
            val second = lookup.factorFor(sonnet, day)

            first.shouldBeNull()
            second.shouldBeNull()
            verify(exactly = 1) { repository.co2FactorsFor(sonnet, day) }
        }

        test("queries the repository per distinct model family") {
            val repository = mockk<Co2FactorRepositoryPort>()
            every { repository.co2FactorsFor(any(), any()) } returns factors
            val lookup = Co2FactorLookup(repository)

            lookup.factorFor(sonnet, day)
            lookup.factorFor(haiku, day)

            verify(exactly = 2) { repository.co2FactorsFor(any(), any()) }
        }
    })
