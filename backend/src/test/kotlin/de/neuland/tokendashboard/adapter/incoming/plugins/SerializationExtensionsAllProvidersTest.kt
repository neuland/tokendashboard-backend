package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.domain.model.AllProviderSeriesBucket
import de.neuland.tokendashboard.domain.model.AllProviderTotals
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.ModelUsages
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.ProviderTotalsByProvider
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageBucket
import de.neuland.tokendashboard.domain.model.UsageTotals
import de.neuland.tokendashboard.domain.model.UserCount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class SerializationExtensionsAllProvidersTest :
    FunSpec({
        fun bucket(date: LocalDate) =
            UsageBucket(
                date = Day(date),
                modelUsages = ModelUsages.NoUsage,
                pluginInstallations = UserCount(0),
            )

        fun totals(
            inputTokens: Long,
            outputTokens: Long,
            cent: Long,
            gram: Double,
        ) = UsageTotals(
            tokens = TokenCounts(TokenCount(inputTokens), TokenCount(outputTokens), TokenCount(1_000), TokenCount(1_000)),
            costUsdCent = DollarCent(cent),
            estimatedCO2 = Co2Gram(gram),
        )

        test("maps each provider's totals, counting only input and output tokens") {
            val range = DayRange.of(Day(LocalDate.of(2026, 6, 1)), Day(LocalDate.of(2026, 6, 30)))
            val totalsByProvider =
                mapOf(
                    CLAUDE to totals(inputTokens = 40, outputTokens = 60, cent = 1, gram = 10.0),
                    COPILOT to totals(inputTokens = 200, outputTokens = 0, cent = 2, gram = 20.0),
                    OPENCODE to totals(inputTokens = 100, outputTokens = 200, cent = 3, gram = 30.0),
                )
            val usage = AllProviderTotals(range, ProviderTotalsByProvider.forAllProviders { totalsByProvider.getValue(it) })

            val dto = usage.toResponse()

            dto.from shouldBe "2026-06-01"
            dto.to shouldBe "2026-06-30"
            dto.providerUsages.keys shouldBe setOf("CLAUDE", "COPILOT", "OPENCODE")
            dto.providerUsages.getValue("CLAUDE").tokensInOut shouldBe 100
            dto.providerUsages.getValue("COPILOT").tokensInOut shouldBe 200
            dto.providerUsages.getValue("OPENCODE").tokensInOut shouldBe 300
            dto.providerUsages.getValue("OPENCODE").totalCostUsdCent shouldBe 3
            dto.providerUsages.getValue("OPENCODE").totalEstimatedCo2 shouldBe 30.0
        }

        test("maps each provider's own bucket separately, with no cross-provider sum") {
            val allBucket =
                AllProviderSeriesBucket
                    .byDate(
                        mapOf(
                            CLAUDE to listOf(bucket(LocalDate.of(2026, 6, 15))),
                            COPILOT to listOf(bucket(LocalDate.of(2026, 6, 15))),
                        ),
                    ).single()

            val dto = allBucket.toResponse()

            dto.date shouldBe "2026-06-15"
            dto.providers.keys shouldBe setOf("CLAUDE", "COPILOT")
        }

        test("maps a bucket containing a single provider") {
            val allBucket = AllProviderSeriesBucket.byDate(mapOf(CLAUDE to listOf(bucket(LocalDate.of(2026, 6, 15))))).single()

            val dto = allBucket.toResponse()

            dto.providers.keys shouldHaveSize 1
            dto.providers.keys shouldBe setOf("CLAUDE")
        }
    })
