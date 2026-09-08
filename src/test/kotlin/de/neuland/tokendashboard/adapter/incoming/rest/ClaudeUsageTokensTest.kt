package de.neuland.tokendashboard.adapter.incoming.rest

import de.neuland.tokendashboard.domain.model.IncomingCacheWriteTokens
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ClaudeUsageTokensTest :
    FunSpec({
        test("should set use generic input tokens") {
            val tokens =
                ClaudeUsageTokens(
                    1L,
                    1L,
                    0L,
                    0L,
                    1L,
                    1L,
                )

            val result: IncomingCacheWriteTokens = tokens.toIncomingCacheWriteTokens()

            result.generic.value shouldBe 1L
            result.tokens5m.value shouldBe 0L
            result.tokens1h.value shouldBe 0L
        }

        test("should not add generic cache token if sums match - 5 minutes") {
            val tokens =
                ClaudeUsageTokens(
                    1L,
                    1L,
                    1L,
                    0L,
                    1L,
                    1L,
                )

            val result: IncomingCacheWriteTokens = tokens.toIncomingCacheWriteTokens()

            result.generic.value shouldBe 0L
            result.tokens5m.value shouldBe 1L
            result.tokens1h.value shouldBe 0L
        }

        test("should not add generic cache token if sums match - 1 hour") {
            val tokens =
                ClaudeUsageTokens(
                    1L,
                    1L,
                    0L,
                    1L,
                    1L,
                    1L,
                )

            val result: IncomingCacheWriteTokens = tokens.toIncomingCacheWriteTokens()

            result.generic.value shouldBe 0L
            result.tokens5m.value shouldBe 0L
            result.tokens1h.value shouldBe 1L
        }

        test("should add generic cache token if sums do not match") {
            val tokens =
                ClaudeUsageTokens(
                    1L,
                    1L,
                    1L,
                    1L,
                    3L,
                    1L,
                )

            val result: IncomingCacheWriteTokens = tokens.toIncomingCacheWriteTokens()

            result.generic.value shouldBe 1L
            result.tokens5m.value shouldBe 1L
            result.tokens1h.value shouldBe 1L
        }

        test("should not set negative tokens") {
            val tokens =
                ClaudeUsageTokens(
                    1L,
                    1L,
                    1L,
                    1L,
                    1L,
                    1L,
                )

            val result: IncomingCacheWriteTokens = tokens.toIncomingCacheWriteTokens()

            result.generic.value shouldBe 0L
            result.tokens5m.value shouldBe 1L
            result.tokens1h.value shouldBe 1L
        }
    })
