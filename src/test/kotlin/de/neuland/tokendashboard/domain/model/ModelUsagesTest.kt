package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ModelUsagesTest :
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
            emptyList<ModelUsage>().toModelUsages() shouldBe ModelUsages.NoUsage
        }

        test("a non-empty list becomes HasUsage with every entry") {
            val result = listOf(modelUsage("claude-sonnet-4-5"), modelUsage("claude-opus-4-5")).toModelUsages()

            val hasUsage = result.shouldBeInstanceOf<ModelUsages.HasUsage>()
            hasUsage.entries.size shouldBe 2
        }

        test("a single-element list becomes HasUsage, not NoUsage") {
            listOf(modelUsage("claude-sonnet-4-5")).toModelUsages().shouldBeInstanceOf<ModelUsages.HasUsage>()
        }

        test("plus with NoUsage on either side returns the other side") {
            val usage = listOf(modelUsage("claude-sonnet-4-5")).toModelUsages()

            (ModelUsages.NoUsage + usage) shouldBe usage
            (usage + ModelUsages.NoUsage) shouldBe usage
            (ModelUsages.NoUsage + ModelUsages.NoUsage) shouldBe ModelUsages.NoUsage
        }

        test("plus sums entries of the same model") {
            val result = listOf(modelUsage("claude-sonnet-4-5")).toModelUsages() + listOf(modelUsage("claude-sonnet-4-5")).toModelUsages()

            result shouldBe
                listOf(
                    ModelUsage(
                        model = ModelName.from("claude-sonnet-4-5"),
                        tokens =
                            TokenCounts(
                                inputTokens = TokenCount(200),
                                outputTokens = TokenCount(400),
                                cacheReadTokens = TokenCount(20),
                                cacheWriteTokens = TokenCount(40),
                            ),
                        costUsdCent = DollarCent(100),
                        estimatedCO2 = Co2Gram(10.0),
                    ),
                ).toModelUsages()
        }

        test("plus keeps entries of different models side by side") {
            val result = listOf(modelUsage("claude-sonnet-4-5")).toModelUsages() + listOf(modelUsage("claude-opus-4-5")).toModelUsages()

            val hasUsage = result.shouldBeInstanceOf<ModelUsages.HasUsage>()
            hasUsage.entries.map { it.model }.toSet() shouldBe
                setOf(ModelName.from("claude-sonnet-4-5"), ModelName.from("claude-opus-4-5"))
        }

        test("totals of NoUsage are zero") {
            ModelUsages.NoUsage.totals() shouldBe UsageTotals.zero()
        }

        test("totals sum every field across models") {
            val result = listOf(modelUsage("claude-sonnet-4-5"), modelUsage("claude-opus-4-5")).toModelUsages().totals()

            result shouldBe
                UsageTotals(
                    tokens = TokenCounts(TokenCount(200), TokenCount(400), TokenCount(20), TokenCount(40)),
                    costUsdCent = DollarCent(100),
                    estimatedCO2 = Co2Gram(10.0),
                )
        }
    })
