package de.neuland.tokendashboard.domain.model

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class TokenCountTest :
    FunSpec({
        test("accepts a positive count") {
            TokenCount(42).value shouldBe 42L
        }

        test("accepts zero") {
            shouldNotThrowAny { TokenCount(0) }
        }

        test("rejects a negative count") {
            val exception = shouldThrow<IllegalArgumentException> { TokenCount(-1) }

            exception.message shouldContain "TokenCount"
        }
    })
