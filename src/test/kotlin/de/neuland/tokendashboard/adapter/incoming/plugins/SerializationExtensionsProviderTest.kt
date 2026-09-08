package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageResult
import de.neuland.tokendashboard.domain.model.toUsageResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class SerializationExtensionsProviderTest :
    FunSpec({
        fun modelUsage(
            model: String,
            inputTokens: Long,
            outputTokens: Long,
            cacheReadTokens: Long,
            cacheWriteTokens: Long,
            costUsdCent: Long,
            estimatedCo2: Double,
        ) = ModelUsage(
            model = ModelName.from(model),
            tokens =
                TokenCounts(
                    inputTokens = TokenCount(inputTokens),
                    outputTokens = TokenCount(outputTokens),
                    cacheReadTokens = TokenCount(cacheReadTokens),
                    cacheWriteTokens = TokenCount(cacheWriteTokens),
                ),
            costUsdCent = DollarCent(costUsdCent),
            estimatedCO2 = Co2Gram(estimatedCo2),
        )

        // `overallTotalTokens` is absent from the JSON, rather than a zero-filled
        // object, for a provider with no usage at all.
        test("sumUp returns null for no usage") {
            sumUp(UsageResult.NoUsage).shouldBeNull()
        }

        test("sumUp sums every field across models") {
            val usage =
                listOf(
                    modelUsage("claude-sonnet-4-5", 1, 20, 300, 4000, 5, 60.0),
                    modelUsage("claude-opus-4-5", 7, 80, 900, 1000, 11, 22.0),
                ).toUsageResult()

            val result = sumUp(usage)!!

            result.inputTokens shouldBe 8
            result.outputTokens shouldBe 100
            result.cacheReadTokens shouldBe 1200
            result.cacheWriteTokens shouldBe 5000
            result.costUsdCent shouldBe 16
            result.estimatedCo2 shouldBe 82.0
        }

        test("formats claude model names for display") {
            val usage = modelUsage("claude-sonnet-4-5", 1, 1, 1, 1, 1, 1.0)

            usage.toResponse(CLAUDE).model shouldBe "sonnet-4.5"
        }

        test("leaves copilot model names unchanged") {
            val usage = modelUsage("gpt-5.4", 1, 1, 1, 1, 1, 1.0)

            usage.toResponse(COPILOT).model shouldBe "gpt-5.4"
        }

        test("leaves opencode model names unchanged") {
            val usage = modelUsage("deepseek-v4-pro", 1, 1, 1, 1, 1, 1.0)

            usage.toResponse(OPENCODE).model shouldBe "deepseek-v4-pro"
        }

        test("formats a claude model name with no version dashes") {
            val usage = modelUsage("claude-opus", 1, 1, 1, 1, 1, 1.0)

            usage.toResponse(CLAUDE).model shouldBe "opus"
        }
    })
