package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class Co2FactorsTest :
    FunSpec({
        val sonnetFactors =
            Co2Factors(
                inputFactor = Co2Factor(190.0),
                outputFactor = Co2Factor(1140.0),
                cacheReadFactor = Co2Factor(15.2),
                cacheWriteFactor = Co2Factor(250.0),
            )

        val zero = TokenCount(0)

        fun counts(
            input: TokenCount = zero,
            output: TokenCount = zero,
            cacheRead: TokenCount = zero,
            cacheWrite: TokenCount = zero,
        ) = TokenCounts(
            inputTokens = input,
            outputTokens = output,
            cacheReadTokens = cacheRead,
            cacheWriteTokens = cacheWrite,
        )

        test("calculate co2 gram returns zero when all tokens zero") {
            sonnetFactors.calculateCo2Gram(counts()) shouldBe Co2Gram(0.0)
        }

        test("calculate co2 gram applies input factor") {
            // 1_000_000 tokens * 190 / 1_000_000 = 190.0 g
            sonnetFactors.calculateCo2Gram(counts(input = TokenCount(1_000_000))) shouldBe Co2Gram(190.0)
        }

        test("calculate co2 gram applies cache write factor") {
            // same factor as input tokens
            sonnetFactors.calculateCo2Gram(counts(cacheWrite = TokenCount(1_000_000))) shouldBe Co2Gram(250.0)
        }

        test("calculate co2 gram applies output factor") {
            // 1_000_000 tokens * 1140 / 1_000_000 = 1140.0 g
            sonnetFactors.calculateCo2Gram(counts(output = TokenCount(1_000_000))) shouldBe Co2Gram(1140.0)
        }

        test("calculate co2 gram applies cache read factor") {
            // 1_000_000 * 15.2 / 1_000_000 = 15.2 g
            sonnetFactors.calculateCo2Gram(counts(cacheRead = TokenCount(1_000_000))) shouldBe Co2Gram(15.2)
        }

        test("calculate co2 gram sums all components") {
            val result =
                sonnetFactors.calculateCo2Gram(
                    counts(
                        // 0.1 * 190 = 19 g
                        input = TokenCount(100_000),
                        // 0.1 * 1140 = 114 g
                        output = TokenCount(100_000),
                        // 10 * 15,2 = 152 g
                        cacheRead = TokenCount(10_000_000),
                        // 0.2 * 250 = 50 g
                        cacheWrite = TokenCount(200_000),
                    ),
                )
            val expected = Co2Gram(19 + 114 + 152 + 50.0)
            result shouldBe expected
        }
    })
