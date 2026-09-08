package de.neuland.tokendashboard.domain.model

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class UserCountTest :
    FunSpec({
        test("accepts a positive count") {
            UserCount(42).value shouldBe 42
        }

        test("accepts zero") {
            shouldNotThrowAny { UserCount(0) }
        }

        test("rejects a negative count") {
            val exception = shouldThrow<IllegalArgumentException> { UserCount(-1) }

            exception.message shouldContain "UserCount"
        }
    })
