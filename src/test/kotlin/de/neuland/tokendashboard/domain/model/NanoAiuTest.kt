package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NanoAiuTest :
    FunSpec({

        test("toAiu rounds an exact value") {
            NanoAiu(2_000_000_000L).toAiu() shouldBe 2L
        }

        test("toAiu rounds half up") {
            NanoAiu(1_500_000_000L).toAiu() shouldBe 2L
        }

        test("toAiu rounds down below half") {
            NanoAiu(1_499_999_999L).toAiu() shouldBe 1L
        }

        test("toAiu rounds up just below a whole unit") {
            NanoAiu(999_999_999L).toAiu() shouldBe 1L
        }

        test("toAiu handles zero") {
            NanoAiu(0L).toAiu() shouldBe 0L
        }

        test("toAiu rounds negative half values like Math.round (towards positive infinity)") {
            NanoAiu(-1_500_000_000L).toAiu() shouldBe -1L
            NanoAiu(-999_999_999L).toAiu() shouldBe -1L
        }
    })
