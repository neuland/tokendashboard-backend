package de.neuland.tokendashboard.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldHaveLength

class UserIdTest :
    FunSpec({
        test("accepts a non-blank id") {
            UserId("user-1").value shouldBe "user-1"
        }

        test("rejects an empty id") {
            val exception = shouldThrow<IllegalArgumentException> { UserId("") }

            exception.message shouldContain "UserId"
        }

        test("rejects a blank id") {
            shouldThrow<IllegalArgumentException> { UserId("   ") }
        }

        test("accepts an id of exactly the maximum length") {
            UserId("a".repeat(UserId.MAX_LENGTH)).value shouldHaveLength UserId.MAX_LENGTH
        }

        test("rejects an id longer than the maximum length") {
            val exception = shouldThrow<IllegalArgumentException> { UserId("a".repeat(UserId.MAX_LENGTH + 1)) }

            exception.message shouldContain "UserId"
        }
    })
