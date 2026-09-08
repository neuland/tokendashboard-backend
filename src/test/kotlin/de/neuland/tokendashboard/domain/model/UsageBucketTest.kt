package de.neuland.tokendashboard.domain.model

import arrow.core.toNonEmptyListOrNull
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class UsageBucketTest :
    FunSpec({
        test("overallTotal on empty usage returns zero totals") {
            val bucket =
                UsageBucket(
                    date = Day(LocalDate.of(2026, 6, 15)),
                    modelUsages = UsageResult.NoUsage,
                    pluginInstallations = UserCount(0),
                )

            bucket.overallTotal shouldBe UsageTotals.zero()
        }

        test("overallTotal sums tokens, cost and co2 across model usages") {
            val sonnet =
                ModelUsage(
                    model = ModelName.from("sonnet"),
                    tokens =
                        TokenCounts(
                            inputTokens = TokenCount(100),
                            outputTokens = TokenCount(200),
                            cacheReadTokens = TokenCount(10),
                            cacheWriteTokens = TokenCount(20),
                        ),
                    costUsdCent = DollarCent(50),
                    estimatedCO2 = Co2Gram(5.0),
                )
            val haiku =
                ModelUsage(
                    model = ModelName.from("haiku"),
                    tokens =
                        TokenCounts(
                            inputTokens = TokenCount(1),
                            outputTokens = TokenCount(2),
                            cacheReadTokens = TokenCount(3),
                            cacheWriteTokens = TokenCount(4),
                        ),
                    costUsdCent = DollarCent(7),
                    estimatedCO2 = Co2Gram(0.5),
                )

            val bucket =
                UsageBucket(
                    date = Day(LocalDate.of(2026, 6, 15)),
                    modelUsages = UsageResult.HasUsage(listOf(sonnet, haiku).toNonEmptyListOrNull()!!),
                    pluginInstallations = UserCount(0),
                )

            val totals = bucket.overallTotal

            totals.tokens.inputTokens shouldBe TokenCount(101)
            totals.tokens.outputTokens shouldBe TokenCount(202)
            totals.tokens.cacheReadTokens shouldBe TokenCount(13)
            totals.tokens.cacheWriteTokens shouldBe TokenCount(24)
            totals.costUsdCent shouldBe DollarCent(57)
            totals.estimatedCO2 shouldBe Co2Gram(5.5)
        }
    })
