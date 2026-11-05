package de.neuland.tokendashboard.domain.model

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class Co2FactorTest :
    FunSpec({
        test("accepts a positive factor") {
            Co2Factor(42.0).gramCo2PerMillionToken shouldBe 42.0
        }

        test("accepts zero") {
            shouldNotThrowAny { Co2Factor(0.0) }
        }

        test("rejects a negative factor") {
            val exception = shouldThrow<IllegalArgumentException> { Co2Factor(-1.0) }

            exception.message shouldContain "Co2Factor"
        }
    })
