package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class IncomingCacheWriteTokensTest :
    FunSpec({
        test("keeps the unattributed remainder as generic") {
            val tokens =
                IncomingCacheWriteTokens.fromReportedTotals(
                    reportedTotal = TokenCount(100),
                    tokens5m = TokenCount(30),
                    tokens1h = TokenCount(20),
                )

            tokens.generic.value shouldBe 50L
            tokens.tokens5m.value shouldBe 30L
            tokens.tokens1h.value shouldBe 20L
        }

        test("reports no remainder when the buckets add up to the total") {
            val tokens =
                IncomingCacheWriteTokens.fromReportedTotals(
                    reportedTotal = TokenCount(50),
                    tokens5m = TokenCount(30),
                    tokens1h = TokenCount(20),
                )

            tokens.generic.value shouldBe 0L
        }

        test("clamps an inconsistent negative remainder to zero") {
            val tokens =
                IncomingCacheWriteTokens.fromReportedTotals(
                    reportedTotal = TokenCount(10),
                    tokens5m = TokenCount(30),
                    tokens1h = TokenCount(20),
                )

            tokens.generic.value shouldBe 0L
        }

        test("treats a missing total as no cache writes at all") {
            val tokens =
                IncomingCacheWriteTokens.fromReportedTotals(
                    reportedTotal = TokenCount.ZERO,
                    tokens5m = TokenCount.ZERO,
                    tokens1h = TokenCount.ZERO,
                )

            tokens.totalCached().value shouldBe 0L
            tokens.billedAt1hRate().value shouldBe 0L
        }

        test("bills the 1h bucket and the remainder at the 1h rate") {
            val tokens = IncomingCacheWriteTokens(TokenCount(7), TokenCount(11), TokenCount(13))

            tokens.billedAt1hRate().value shouldBe 24L
        }

        test("excludes the 5m bucket from the 1h rate") {
            val tokens = IncomingCacheWriteTokens(TokenCount(1000), TokenCount(0), TokenCount(0))

            tokens.billedAt1hRate().value shouldBe 0L
        }

        test("counts every bucket in totalCached") {
            val tokens = IncomingCacheWriteTokens(TokenCount(7), TokenCount(11), TokenCount(13))

            tokens.totalCached().value shouldBe 31L
        }
    })
