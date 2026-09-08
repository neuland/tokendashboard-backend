package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class UsageResultTest :
    FunSpec({
        fun modelUsage(model: String) =
            ModelUsage(
                model = ModelName.from(model),
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

        test("an empty list becomes NoUsage") {
            emptyList<ModelUsage>().toUsageResult() shouldBe UsageResult.NoUsage
        }

        test("a non-empty list becomes HasUsage with every entry") {
            val result = listOf(modelUsage("claude-sonnet-4-5"), modelUsage("claude-opus-4-5")).toUsageResult()

            val hasUsage = result.shouldBeInstanceOf<UsageResult.HasUsage>()
            hasUsage.entries.size shouldBe 2
        }

        test("a single-element list becomes HasUsage, not NoUsage") {
            listOf(modelUsage("claude-sonnet-4-5")).toUsageResult().shouldBeInstanceOf<UsageResult.HasUsage>()
        }
    })
