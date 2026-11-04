package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class ClaudePriceVersionsTest :
    FunSpec({
        val sonnet = ModelName.from("claude-sonnet-4-5")

        fun prices(inputCent: Long) =
            ClaudePrices(
                inputCentPerMillion = CentPerMillionToken(inputCent),
                outputCentPerMillion = CentPerMillionToken(0),
                cacheWrite5mCentPerMillion = CentPerMillionToken(0),
                cacheWrite1hCentPerMillion = CentPerMillionToken(0),
                cacheReadCentPerMillion = CentPerMillionToken(0),
            )

        fun day(dayOfMonth: Int) = Day(LocalDate.of(2026, 6, dayOfMonth))

        val versions =
            ClaudePriceVersions(
                listOf(
                    ClaudePriceVersion(ModelFamily("sonnet"), day(10), prices(300)),
                    ClaudePriceVersion(ModelFamily("sonnet"), day(1), prices(200)),
                ),
            )

        test("prices for picks the version valid on the given day regardless of input order") {
            versions.pricesFor(sonnet, day(5)) shouldBe prices(200)
            versions.pricesFor(sonnet, day(15)) shouldBe prices(300)
        }

        test("prices for treats validFrom as inclusive") {
            versions.pricesFor(sonnet, day(10)) shouldBe prices(300)
        }

        test("prices for returns ZERO for an unknown model family") {
            versions.pricesFor(ModelName.from("claude-haiku-4-5"), day(15)) shouldBe ClaudePrices.ZERO
        }

        test("prices for falls back to the oldest version for a day before the first version") {
            versions.pricesFor(sonnet, Day(LocalDate.of(2026, 5, 31))) shouldBe prices(200)
        }
    })
