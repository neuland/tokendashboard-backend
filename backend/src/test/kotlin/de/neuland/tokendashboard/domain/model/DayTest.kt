package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class DayTest :
    FunSpec({
        val earlier = Day(LocalDate.of(2026, 6, 14))
        val later = Day(LocalDate.of(2026, 6, 15))

        test("days compare by date") {
            earlier shouldBeLessThan later
            later shouldBeGreaterThan earlier
            earlier.compareTo(Day(LocalDate.of(2026, 6, 14))) shouldBe 0
        }
    })
