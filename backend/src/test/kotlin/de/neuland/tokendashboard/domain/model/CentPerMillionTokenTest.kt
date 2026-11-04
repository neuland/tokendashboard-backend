package de.neuland.tokendashboard.domain.model

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class CentPerMillionTokenTest :
    FunSpec({

        test("cent per token divides value by one million") {
            CentPerMillionToken(1_000_000).centPerToken() shouldBe 1.0
        }

        test("cent per token for zero is zero") {
            CentPerMillionToken(0).centPerToken() shouldBe 0.0
        }

        test("cent per token for fractional cent") {
            CentPerMillionToken(300).centPerToken() shouldBe 0.0003
        }

        test("accepts a positive value") {
            CentPerMillionToken(42).value shouldBe 42L
        }

        test("accepts zero") {
            shouldNotThrowAny { CentPerMillionToken(0) }
        }

        test("rejects a negative value") {
            val exception = shouldThrow<IllegalArgumentException> { CentPerMillionToken(-1) }

            exception.message shouldContain "CentPerMillionToken"
        }
    })
