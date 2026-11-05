package de.neuland.tokendashboard.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldHaveLength

class SessionIdTest :
    FunSpec({
        test("accepts a non-blank id") {
            SessionId("session-1").value shouldBe "session-1"
        }

        test("rejects an empty id") {
            val exception = shouldThrow<IllegalArgumentException> { SessionId("") }

            exception.message shouldContain "SessionId"
        }

        test("rejects a blank id") {
            shouldThrow<IllegalArgumentException> { SessionId("   ") }
        }

        test("accepts an id of exactly the maximum length") {
            SessionId("a".repeat(SessionId.MAX_LENGTH)).value shouldHaveLength SessionId.MAX_LENGTH
        }

        test("rejects an id longer than the maximum length") {
            val exception = shouldThrow<IllegalArgumentException> { SessionId("a".repeat(SessionId.MAX_LENGTH + 1)) }

            exception.message shouldContain "SessionId"
        }
    })
