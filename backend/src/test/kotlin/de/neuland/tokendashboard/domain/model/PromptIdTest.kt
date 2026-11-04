package de.neuland.tokendashboard.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldHaveLength

class PromptIdTest :
    FunSpec({
        test("accepts a non-blank id") {
            PromptId("prompt-1").value shouldBe "prompt-1"
        }

        test("rejects an empty id") {
            val exception = shouldThrow<IllegalArgumentException> { PromptId("") }

            exception.message shouldContain "PromptId"
        }

        test("rejects a blank id") {
            shouldThrow<IllegalArgumentException> { PromptId("   ") }
        }

        test("accepts an id of exactly the maximum length") {
            PromptId("a".repeat(PromptId.MAX_LENGTH)).value shouldHaveLength PromptId.MAX_LENGTH
        }

        test("rejects an id longer than the maximum length") {
            val exception = shouldThrow<IllegalArgumentException> { PromptId("a".repeat(PromptId.MAX_LENGTH + 1)) }

            exception.message shouldContain "PromptId"
        }
    })
