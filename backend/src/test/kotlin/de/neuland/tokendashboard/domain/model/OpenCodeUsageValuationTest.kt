package de.neuland.tokendashboard.domain.model

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class OpenCodeUsageValuationTest :
    FunSpec({
        val day14 = Day(LocalDate.of(2026, 6, 14))
        val day15 = Day(LocalDate.of(2026, 6, 15))
        val model = ModelName.from("deepseek-v4-pro")
        val family = model.family()
        val tokens = TokenCounts(TokenCount(1_000_000), TokenCount.ZERO, TokenCount.ZERO, TokenCount.ZERO)

        fun usage(
            model: ModelName = ModelName.from("deepseek-v4-pro"),
            costUsdCent: Long = 0,
        ) = OpenCodeRawModelUsage(model, tokens, NanoCent(costUsdCent))

        fun inputFactor(gram: Double) = Co2Factors(Co2Factor(gram), Co2Factor.ZERO, Co2Factor.ZERO, Co2Factor.ZERO)

        val valuation =
            OpenCodeUsageValuation(
                Co2FactorVersions(
                    listOf(
                        Co2FactorVersion(family, day14, inputFactor(100.0)),
                        Co2FactorVersion(family, day15, inputFactor(200.0)),
                    ),
                ),
            )

        test("valuate converts nano-cent to cent") {
            valuation.valuate(day14, usage(costUsdCent = 5_000_000_000L)).costUsdCent shouldBe DollarCent(5)
        }

        test("valuate rounds cost instead of truncating it") {
            valuation.valuate(day14, usage(costUsdCent = 12_900_000_000L)).costUsdCent shouldBe DollarCent(13)
        }

        test("valuate keeps the tokens and applies the co2 factor valid on the given day") {
            valuation.valuate(day14, usage()) shouldBe ModelUsage(model, tokens, DollarCent(0), Co2Gram(100.0))
            valuation.valuate(day15, usage()).estimatedCO2 shouldBe Co2Gram(200.0)
        }

        test("valuate sets co2 to zero for a model family without co2 factors") {
            valuation.valuate(day14, usage(model = ModelName.from("unknown-model"))).estimatedCO2 shouldBe Co2Gram(0.0)
        }

        fun valued(
            day: Day,
            gram: Double,
        ) = DatedModelUsages(day, ModelUsages.HasUsage(nonEmptyListOf(ModelUsage(model, tokens, DollarCent(0), Co2Gram(gram)))))

        test("valuate of tokens by day keeps one group per day") {
            valuation.valuate(mapOf(day15 to listOf(usage()), day14 to listOf(usage()))) shouldBe
                listOf(valued(day15, 200.0), valued(day14, 100.0))
        }
    })
