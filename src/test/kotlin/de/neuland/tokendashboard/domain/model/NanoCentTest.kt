package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NanoCentTest :
    FunSpec({

        test("toCent rounds an exact value") {
            NanoCent(2_000_000_000L).toCent().value shouldBe 2L
        }

        test("toCent rounds half up") {
            NanoCent(1_500_000_000L).toCent().value shouldBe 2L
        }

        test("toCent rounds down below half") {
            NanoCent(1_499_999_999L).toCent().value shouldBe 1L
        }

        test("fromDollar converts dollars to nano-cent") {
            NanoCent.fromDollar(1.23).value shouldBe 123_000_000_000L
        }

        test("fromDollar rounds fractional nano-cent") {
            // 0.000000000004 dollar * 100 * 1e9 = 0.4 nano-cent -> rounds to 0
            NanoCent.fromDollar(0.00000000004).value shouldBe 4L
        }

        test("fromDollar rounds fractional nano-cent to zero") {
            // 0.000000000004 dollar * 100 * 1e9 = 0.4 nano-cent -> rounds to 0
            NanoCent.fromDollar(0.000000000004).value shouldBe 0L
        }

        test("sum of nano-cent values stays exact before rounding to cent") {
            val sum = NanoCent(500_000_000L) + NanoCent(500_000_000L)
            sum.toCent().value shouldBe 1L
        }
    })
