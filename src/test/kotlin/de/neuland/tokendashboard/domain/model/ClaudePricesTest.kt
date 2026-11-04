package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ClaudePricesTest :
    FunSpec({

        val prices =
            ClaudePrices(
                inputCentPerMillion = CentPerMillionToken(300),
                outputCentPerMillion = CentPerMillionToken(1500),
                cacheWrite5mCentPerMillion = CentPerMillionToken(375),
                cacheWrite1hCentPerMillion = CentPerMillionToken(600),
                cacheReadCentPerMillion = CentPerMillionToken(30),
            )

        test("calculate cost sums weighted token counts") {
            val tokens =
                ClaudeTokenCounts(
                    inputTokens = TokenCount(1_000_000),
                    outputTokens = TokenCount(1_000_000),
                    cacheReadTokens = TokenCount(1_000_000),
                    cacheWriteTokens5m = TokenCount(1_000_000),
                    cacheWriteTokens1h = TokenCount.ZERO,
                    cacheWriteTokensGeneric = TokenCount.ZERO,
                )

            prices.calculateCost(tokens) shouldBe DollarCent(300 + 1500 + 375 + 30)
        }

        test("calculate cost applies distinct rate per cache write duration") {
            val tokens =
                ClaudeTokenCounts(
                    inputTokens = TokenCount(0),
                    outputTokens = TokenCount(0),
                    cacheReadTokens = TokenCount(0),
                    cacheWriteTokens5m = TokenCount(1_000_000),
                    cacheWriteTokens1h = TokenCount(1_000_000),
                    cacheWriteTokensGeneric = TokenCount(1_000_000),
                )

            prices.calculateCost(tokens) shouldBe DollarCent(375 + 600 + 600)
        }

        test("calculate cost rounds half up at cent boundary") {
            // 3 tokens * 300 cent/1e6 = 0.0009, plus a single output token chosen so the
            // sum lands exactly on x.5 cent: 1 token * 500_000 cent/1e6 = 0.5 cent
            val tokens =
                ClaudeTokenCounts(
                    inputTokens = TokenCount(0),
                    outputTokens = TokenCount(1),
                    cacheReadTokens = TokenCount(0),
                    cacheWriteTokens5m = TokenCount.ZERO,
                    cacheWriteTokens1h = TokenCount.ZERO,
                    cacheWriteTokensGeneric = TokenCount.ZERO,
                )
            val halfCentPrices =
                ClaudePrices(
                    inputCentPerMillion = CentPerMillionToken(0),
                    outputCentPerMillion = CentPerMillionToken(500_000),
                    cacheWrite5mCentPerMillion = CentPerMillionToken(0),
                    cacheWrite1hCentPerMillion = CentPerMillionToken(0),
                    cacheReadCentPerMillion = CentPerMillionToken(0),
                )

            halfCentPrices.calculateCost(tokens) shouldBe DollarCent(1)
        }

        test("calculate costs for small values") {
            val tokens =
                ClaudeTokenCounts(
                    inputTokens = TokenCount(10),
                    outputTokens = TokenCount(100),
                    cacheReadTokens = TokenCount(1000),
                    cacheWriteTokens5m = TokenCount(5000),
                    cacheWriteTokens1h = TokenCount(5000),
                    cacheWriteTokensGeneric = TokenCount.ZERO,
                )
            val halfCentPrices =
                ClaudePrices(
                    inputCentPerMillion = CentPerMillionToken(500),
                    outputCentPerMillion = CentPerMillionToken(500),
                    cacheReadCentPerMillion = CentPerMillionToken(500),
                    cacheWrite5mCentPerMillion = CentPerMillionToken(500),
                    cacheWrite1hCentPerMillion = CentPerMillionToken(500),
                )
            // 11110*500 = 5555000 = 5.5
            halfCentPrices.calculateCost(tokens) shouldBe DollarCent(6)
        }
    })
