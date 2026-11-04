package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class GranularityTest :
    FunSpec({
        val monday = Day(LocalDate.of(2026, 6, 15))
        val wednesday = Day(LocalDate.of(2026, 6, 17))
        val sunday = Day(LocalDate.of(2026, 6, 21))

        test("DAY buckets every day as itself") {
            Granularity.DAY.bucketStart(wednesday) shouldBe wednesday
        }

        test("WEEK buckets a monday as itself") {
            Granularity.WEEK.bucketStart(monday) shouldBe monday
        }

        test("WEEK buckets midweek days and sunday to the preceding monday") {
            Granularity.WEEK.bucketStart(wednesday) shouldBe monday
            Granularity.WEEK.bucketStart(sunday) shouldBe monday
        }
    })
