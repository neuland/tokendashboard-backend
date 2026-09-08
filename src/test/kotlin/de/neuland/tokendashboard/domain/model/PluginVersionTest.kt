package de.neuland.tokendashboard.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldHaveLength

class PluginVersionTest :
    FunSpec({
        test("accepts a normal version string") {
            PluginVersion("1.2.3").value shouldBe "1.2.3"
        }

        test("rejects an empty version") {
            val exception = shouldThrow<IllegalArgumentException> { PluginVersion("") }

            exception.message shouldContain "PluginVersion"
        }

        test("rejects a blank version") {
            shouldThrow<IllegalArgumentException> { PluginVersion("   ") }
        }

        test("accepts a version of exactly the maximum length") {
            PluginVersion("a".repeat(PluginVersion.MAX_LENGTH)).value shouldHaveLength PluginVersion.MAX_LENGTH
        }

        test("rejects a version longer than the maximum length") {
            val exception = shouldThrow<IllegalArgumentException> { PluginVersion("a".repeat(PluginVersion.MAX_LENGTH + 1)) }

            exception.message shouldContain "PluginVersion"
        }
    })
