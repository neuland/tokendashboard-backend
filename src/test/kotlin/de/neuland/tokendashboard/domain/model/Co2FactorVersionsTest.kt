package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class Co2FactorVersionsTest :
    FunSpec({
        val sonnet = ModelName.from("claude-sonnet-4-5")

        fun factors(input: Double) =
            Co2Factors(
                inputFactor = Co2Factor(input),
                outputFactor = Co2Factor.ZERO,
                cacheReadFactor = Co2Factor.ZERO,
                cacheWriteFactor = Co2Factor.ZERO,
            )

        fun day(dayOfMonth: Int) = Day(LocalDate.of(2026, 6, dayOfMonth))

        val versions =
            Co2FactorVersions(
                listOf(
                    Co2FactorVersion(ModelFamily("sonnet"), day(10), factors(300.0)),
                    Co2FactorVersion(ModelFamily("sonnet"), day(1), factors(200.0)),
                ),
            )

        test("co2 factors for picks the version valid on the given day regardless of input order") {
            versions.co2FactorsFor(sonnet, day(5)) shouldBe factors(200.0)
            versions.co2FactorsFor(sonnet, day(15)) shouldBe factors(300.0)
        }

        test("co2 factors for treats validFrom as inclusive") {
            versions.co2FactorsFor(sonnet, day(10)) shouldBe factors(300.0)
        }

        test("co2 factors for returns ZERO for an unknown model family") {
            versions.co2FactorsFor(ModelName.from("claude-haiku-4-5"), day(15)) shouldBe Co2Factors.ZERO
        }

        test("co2 factors for falls back to the oldest version for a day before the first version") {
            versions.co2FactorsFor(sonnet, Day(LocalDate.of(2026, 5, 31))) shouldBe factors(200.0)
        }
    })
