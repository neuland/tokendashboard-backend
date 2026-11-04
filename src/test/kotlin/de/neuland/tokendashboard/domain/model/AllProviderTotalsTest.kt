package de.neuland.tokendashboard.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AllProviderTotalsTest :
    FunSpec({
        test("tokens in out counts only input and output tokens") {
            val totals =
                UsageTotals(
                    tokens = TokenCounts(TokenCount(1), TokenCount(2), TokenCount(400), TokenCount(800)),
                    costUsdCent = DollarCent(16),
                    estimatedCO2 = Co2Gram(8.5),
                )

            totals.tokensInOut() shouldBe TokenCount(3)
        }

        test("for all providers holds the totals of every provider exactly once, keyed by provider") {
            val byProvider = ProviderTotalsByProvider.forAllProviders { UsageTotals.zero() }.byProvider

            byProvider.keys shouldBe Provider.entries.toSet()
        }

        test("for all providers asks for the totals of each provider") {
            val tokensByProvider = mapOf(Provider.CLAUDE to 1L, Provider.COPILOT to 2L, Provider.OPENCODE to 3L)

            val byProvider =
                ProviderTotalsByProvider
                    .forAllProviders { provider ->
                        UsageTotals.zero().copy(
                            tokens = TokenCounts.zero().copy(inputTokens = TokenCount(tokensByProvider.getValue(provider))),
                        )
                    }.byProvider

            byProvider.mapValues { it.value.tokensInOut() } shouldBe tokensByProvider.mapValues { TokenCount(it.value) }
        }
    })
