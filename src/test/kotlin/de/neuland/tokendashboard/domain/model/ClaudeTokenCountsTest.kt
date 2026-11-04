package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ClaudeTokenCountsTest :
    FunSpec({
        val tokens =
            ClaudeTokenCounts(
                inputTokens = TokenCount(1),
                outputTokens = TokenCount(2),
                cacheReadTokens = TokenCount(4),
                cacheWriteTokens5m = TokenCount(8),
                cacheWriteTokens1h = TokenCount(16),
                cacheWriteTokensGeneric = TokenCount(32),
            )

        test("total cached sums all cached buckets") {
            tokens.totalCached() shouldBe TokenCount(4 + 8 + 16 + 32)
        }

        test("cache write sums all cache write buckets") {
            tokens.cacheWrite() shouldBe TokenCount(8 + 16 + 32)
        }

        test("cache billed at 1h rate sums 1h and generic cache writes") {
            tokens.cacheWriteBilledAt1hRate() shouldBe TokenCount(16 + 32)
        }

        test("to token counts merges all cache write buckets into one") {
            tokens.toTokenCounts() shouldBe
                TokenCounts(
                    inputTokens = TokenCount(1),
                    outputTokens = TokenCount(2),
                    cacheReadTokens = TokenCount(4),
                    cacheWriteTokens = TokenCount(8 + 16 + 32),
                )
        }
    })
