package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.domain.model.AllProviderSeriesBucket
import de.neuland.tokendashboard.domain.model.AllProviderUsage
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.ProviderUsage
import de.neuland.tokendashboard.domain.model.ProviderUsages
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.UsageBucket
import de.neuland.tokendashboard.domain.model.UsageResult
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
                modelUsages = UsageResult.NoUsage,
                pluginInstallations = UserCount(0),
            )

        test("maps each provider's totals") {
            val range = DayRange.of(Day(LocalDate.of(2026, 6, 1)), Day(LocalDate.of(2026, 6, 30)))
            val usage =
                AllProviderUsage(
                    range = range,
                    providerUsages =
                        ProviderUsages.of(
                            listOf(
                                ProviderUsage(CLAUDE, TokenCount(100), Co2Gram(10.0), DollarCent(1)),
                                ProviderUsage(COPILOT, TokenCount(200), Co2Gram(20.0), DollarCent(2)),
                                ProviderUsage(OPENCODE, TokenCount(300), Co2Gram(30.0), DollarCent(3)),
                            ),
                        ),
                )

            val dto = usage.toResponse()

            dto.from shouldBe "2026-06-01"
            dto.to shouldBe "2026-06-30"
            dto.providerUsages.keys shouldBe setOf("CLAUDE", "COPILOT", "OPENCODE")
            dto.providerUsages.getValue("CLAUDE").tokensInOut shouldBe 100
            dto.providerUsages.getValue("COPILOT").tokensInOut shouldBe 200
            dto.providerUsages.getValue("OPENCODE").tokensInOut shouldBe 300
        }

        test("maps each provider's own bucket separately, with no cross-provider sum") {
            val allBucket =
                AllProviderSeriesBucket(
                    date = Day(LocalDate.of(2026, 6, 15)),
                    providerBuckets =
                        mapOf(
                            CLAUDE to bucket(LocalDate.of(2026, 6, 15)),
                            COPILOT to bucket(LocalDate.of(2026, 6, 15)),
                        ),
                )

            val dto = allBucket.toResponse()

            dto.date shouldBe "2026-06-15"
            dto.providers.keys shouldBe setOf("CLAUDE", "COPILOT")
        }

        test("maps a bucket containing a single provider") {
            val allBucket =
                AllProviderSeriesBucket(
                    date = Day(LocalDate.of(2026, 6, 15)),
                    providerBuckets = mapOf(CLAUDE to bucket(LocalDate.of(2026, 6, 15))),
                )

            val dto = allBucket.toResponse()

            dto.providers.keys shouldHaveSize 1
            dto.providers.keys shouldBe setOf("CLAUDE")
        }
    })
