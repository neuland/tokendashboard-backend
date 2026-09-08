package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
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

class PriceLookupTest :
    FunSpec({
        val day = Day(LocalDate.of(2026, 6, 15))
        val sonnet = ModelFamily("sonnet")
        val haiku = ModelFamily("haiku")

        val prices =
            ClaudePrices(
                inputCentPerMillion = CentPerMillionToken(300L),
                outputCentPerMillion = CentPerMillionToken(1500L),
                cacheWrite5mCentPerMillion = CentPerMillionToken(375L),
                cacheWrite1hCentPerMillion = CentPerMillionToken(600L),
                cacheReadCentPerMillion = CentPerMillionToken(30L),
            )

        test("returns the price from the repository") {
            val repository = mockk<ClaudePriceRepositoryPort>()
            every { repository.priceFor(any(), any()) } returns prices
            val lookup = PriceLookup(repository)

            val result = lookup.priceFor(sonnet, day)

            result shouldBe prices
        }

        test("queries the repository only once per model family and day") {
            val repository = mockk<ClaudePriceRepositoryPort>()
            every { repository.priceFor(any(), any()) } returns prices
            val lookup = PriceLookup(repository)

            lookup.priceFor(sonnet, day)
            lookup.priceFor(sonnet, day)

            verify(exactly = 1) { repository.priceFor(any(), any()) }
        }

        test("caches a missing price and does not query again") {
            val repository = mockk<ClaudePriceRepositoryPort>()
            every { repository.priceFor(any(), any()) } returns null
            val lookup = PriceLookup(repository)

            val first = lookup.priceFor(sonnet, day)
            val second = lookup.priceFor(sonnet, day)

            first.shouldBeNull()
            second.shouldBeNull()
            verify(exactly = 1) { repository.priceFor(sonnet, day) }
        }

        test("queries the repository per distinct model family") {
            val repository = mockk<ClaudePriceRepositoryPort>()
            every { repository.priceFor(any(), any()) } returns prices
            val lookup = PriceLookup(repository)

            lookup.priceFor(sonnet, day)
            lookup.priceFor(haiku, day)

            verify(exactly = 2) { repository.priceFor(any(), any()) }
        }
    })
