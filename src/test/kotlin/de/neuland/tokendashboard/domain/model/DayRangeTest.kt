package de.neuland.tokendashboard.domain.model

import de.neuland.tokendashboard.domain.model.DayRange.Companion.MAX_RANGE_DAYS
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

class DayRangeTest :
    FunSpec({
        val from = Day(LocalDate.of(2026, 6, 1))

        test("keeps from and to of an ordered range") {
            val to = Day(LocalDate.of(2026, 6, 30))

            val range = DayRange.of(from, to)

            range.from shouldBe from
            range.to shouldBe to
        }

        test("accepts a range of a single day") {
            val range = DayRange.of(from, from)

            range.from shouldBe from
            range.to shouldBe from
        }

        test("rejects a range where from is after to") {
            val to = Day(LocalDate.of(2026, 5, 31))

            // when / then
            val exception = shouldThrow<IllegalArgumentException> { DayRange.of(from, to) }
            exception.message shouldContain "range must be ordered"
        }

        test("accepts a range spanning exactly the maximum number of days") {
            val to = Day(from.value.plusDays(MAX_RANGE_DAYS))

            val range = DayRange.of(from, to)

            range.to shouldBe to
        }

        test("rejects a range spanning more than the maximum number of days") {
            val to = Day(from.value.plusDays(MAX_RANGE_DAYS + 1))

            // when / then
            val exception = shouldThrow<IllegalArgumentException> { DayRange.of(from, to) }
            exception.message shouldContain "range must span at most"
        }

        test("rejects an absurdly large range") {
            val to = Day(LocalDate.MAX)

            // when / then
            shouldThrow<IllegalArgumentException> { DayRange.of(from, to) }
        }
    })
