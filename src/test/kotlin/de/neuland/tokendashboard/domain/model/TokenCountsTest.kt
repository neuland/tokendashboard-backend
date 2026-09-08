package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TokenCountsTest :
    FunSpec({

        val counts =
            TokenCounts(
                inputTokens = TokenCount(100),
                outputTokens = TokenCount(50),
                cacheReadTokens = TokenCount(10),
                cacheWriteTokens = TokenCount(5),
            )

        test("plus adds field wise") {
            val other =
                TokenCounts(
                    inputTokens = TokenCount(1),
                    outputTokens = TokenCount(2),
                    cacheReadTokens = TokenCount(3),
                    cacheWriteTokens = TokenCount(4),
                )

            val sum = counts + other

            sum.inputTokens shouldBe TokenCount(101)
            sum.outputTokens shouldBe TokenCount(52)
            sum.cacheReadTokens shouldBe TokenCount(13)
            sum.cacheWriteTokens shouldBe TokenCount(9)
        }

        test("sum in and out adds input and output") {
            counts.sumInAndOut() shouldBe TokenCount(150)
        }

        test("sum over all adds all four fields") {
            counts.sumOverAll() shouldBe TokenCount(165)
        }
    })
