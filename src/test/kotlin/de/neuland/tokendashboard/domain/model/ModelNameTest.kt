package de.neuland.tokendashboard.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ModelNameTest :
    FunSpec({
        test("from() accepts a model name of exactly the maximum length") {
            ModelName.from("a".repeat(ModelName.MAX_LENGTH)).value shouldBe "a".repeat(ModelName.MAX_LENGTH)
        }

        test("from() rejects a model name longer than the maximum length") {
            val exception = shouldThrow<IllegalArgumentException> { ModelName.from("a".repeat(ModelName.MAX_LENGTH + 1)) }

            exception.message shouldContain "ModelName"
        }

        test("restore() does not enforce the maximum length, since it only rehydrates already-stored rows") {
            val oversized = "a".repeat(ModelName.MAX_LENGTH + 1)
            ModelName.restore(oversized).value shouldBe oversized
        }

        withData(
            "gpt-4.1-2025-04-14" to "gpt-4.1",
            "claude-haiku-4-5-20251001" to "claude-haiku-4-5",
        ) { (input, expected) ->
            ModelName.from(input).value shouldBe expected
        }
        withData(
            "gpt-4.1-2025-04-14" to "gpt-4",
            "gpt-5" to "gpt-5",
            "gpt-5.4" to "gpt-5",
            "gpt-5.4-codex" to "gpt-5-codex",
            "gpt-5.4-mini" to "gpt-5-mini",
            "gpt-5-mini" to "gpt-5-mini",
            "gpt-14" to "gpt-14",
            "gpt-14-mini" to "gpt-14-mini",
            "gpt-14.2-mini" to "gpt-14-mini",
            "claude-haiku-4-5-20251001" to "haiku",
            "claude-sonnet-4-6" to "sonnet",
            "claude-sonnet-5" to "sonnet",
            "gpt" to "gpt",
            "gpt4" to "gpt4",
        ) { (input, expected) ->
            ModelName.from(input).family() shouldBe ModelFamily(expected)
        }
    })
