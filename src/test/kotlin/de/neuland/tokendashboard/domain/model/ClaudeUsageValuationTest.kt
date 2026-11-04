package de.neuland.tokendashboard.domain.model

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class ClaudeUsageValuationTest :
    FunSpec({
        val day14 = Day(LocalDate.of(2026, 6, 14))
        val day15 = Day(LocalDate.of(2026, 6, 15))
        val sonnet = ModelName.from("claude-sonnet-4-5")
        val haiku = ModelName.from("claude-haiku-4-5")
        val zero = TokenCount.ZERO

        fun tokens(
            input: Long = 0,
            cacheRead: Long = 0,
            cacheWrite5m: Long = 0,
            cacheWrite1h: Long = 0,
            cacheWriteGeneric: Long = 0,
        ) = ClaudeTokenCounts(
            inputTokens = TokenCount(input),
            outputTokens = zero,
            cacheReadTokens = TokenCount(cacheRead),
            cacheWriteTokens5m = TokenCount(cacheWrite5m),
            cacheWriteTokens1h = TokenCount(cacheWrite1h),
            cacheWriteTokensGeneric = TokenCount(cacheWriteGeneric),
        )

        fun inputPrice(cent: Long) =
            ClaudePrices(
                inputCentPerMillion = CentPerMillionToken(cent),
                outputCentPerMillion = CentPerMillionToken.ZERO,
                cacheWrite5mCentPerMillion = CentPerMillionToken.ZERO,
                cacheWrite1hCentPerMillion = CentPerMillionToken.ZERO,
                cacheReadCentPerMillion = CentPerMillionToken.ZERO,
            )

        fun inputFactor(gram: Double) = Co2Factors(Co2Factor(gram), Co2Factor.ZERO, Co2Factor.ZERO, Co2Factor.ZERO)

        val valuation =
            ClaudeUsageValuation(
                prices =
                    ClaudePriceVersions(
                        listOf(
                            ClaudePriceVersion(ModelFamily("sonnet"), day14, inputPrice(300)),
                            ClaudePriceVersion(ModelFamily("sonnet"), day15, inputPrice(600)),
                        ),
                    ),
                co2Factors =
                    Co2FactorVersions(
                        listOf(
                            Co2FactorVersion(ModelFamily("sonnet"), day14, inputFactor(100.0)),
                            Co2FactorVersion(ModelFamily("sonnet"), day15, inputFactor(200.0)),
                        ),
                    ),
            )

        fun inputUsage(
            model: ModelName,
            cent: Long,
            gram: Double,
        ) = ModelUsage(
            model = model,
            tokens = TokenCounts(TokenCount(1_000_000), zero, zero, zero),
            costUsdCent = DollarCent(cent),
            estimatedCO2 = Co2Gram(gram),
        )

        test("valuate applies the price and co2 factor valid on the given day") {
            valuation.valuate(day14, ClaudeRawModelUsage(sonnet, tokens(input = 1_000_000))) shouldBe inputUsage(sonnet, 300, 100.0)
            valuation.valuate(day15, ClaudeRawModelUsage(sonnet, tokens(input = 1_000_000))) shouldBe inputUsage(sonnet, 600, 200.0)
        }

        test("valuate keeps tokens but zeroes cost and co2 for a model family without versions") {
            valuation.valuate(day14, ClaudeRawModelUsage(haiku, tokens(input = 1_000_000))) shouldBe inputUsage(haiku, 0, 0.0)
        }

        test("valuate merges all cache write buckets into cache write tokens") {
            val result =
                valuation.valuate(
                    day14,
                    ClaudeRawModelUsage(sonnet, tokens(cacheWrite5m = 1, cacheWrite1h = 2, cacheWriteGeneric = 4)),
                )

            result.tokens.cacheWriteTokens shouldBe TokenCount(1 + 2 + 4)
        }

        test("valuate applies the cache read co2 factor only to cache reads and the cache write factor to every cache write bucket") {
            val factors = Co2Factors(Co2Factor.ZERO, Co2Factor.ZERO, cacheReadFactor = Co2Factor(10.0), cacheWriteFactor = Co2Factor(100.0))
            val co2Only =
                ClaudeUsageValuation(
                    ClaudePriceVersions(emptyList()),
                    Co2FactorVersions(listOf(Co2FactorVersion(ModelFamily("sonnet"), day14, factors))),
                )
            val usage =
                ClaudeRawModelUsage(
                    sonnet,
                    tokens(cacheRead = 1_000_000, cacheWrite5m = 200_000, cacheWrite1h = 300_000, cacheWriteGeneric = 500_000),
                )

            co2Only.valuate(day14, usage).estimatedCO2 shouldBe Co2Gram(10.0 + 100.0)
        }

        test("valuate of tokens by day keeps one group per day") {
            val result =
                valuation.valuate(
                    mapOf(
                        day15 to listOf(ClaudeRawModelUsage(sonnet, tokens(input = 1_000_000))),
                        day14 to
                            listOf(
                                ClaudeRawModelUsage(sonnet, tokens(input = 1_000_000)),
                                ClaudeRawModelUsage(haiku, tokens(input = 1_000_000)),
                            ),
                    ),
                )

            result shouldBe
                listOf(
                    DatedModelUsages(day15, ModelUsages.HasUsage(nonEmptyListOf(inputUsage(sonnet, 600, 200.0)))),
                    DatedModelUsages(
                        day14,
                        ModelUsages.HasUsage(nonEmptyListOf(inputUsage(sonnet, 300, 100.0), inputUsage(haiku, 0, 0.0))),
                    ),
                )
        }

        test("valuate of no tokens is empty") {
            valuation.valuate(emptyMap()).shouldBeEmpty()
        }
    })
