package de.neuland.tokendashboard.integrationtests

import de.neuland.tokendashboard.adapter.incoming.plugins.ClaudePriceVersionDto
import de.neuland.tokendashboard.adapter.incoming.plugins.ClaudePricesDto
import de.neuland.tokendashboard.adapter.incoming.plugins.Co2FactorVersionDto
import de.neuland.tokendashboard.adapter.incoming.plugins.Co2FactorsDto
import de.neuland.tokendashboard.testsupport.configureIntegrationTestApp
import de.neuland.tokendashboard.testsupport.fixedTimeFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.jdbi.v3.core.Jdbi
import java.time.LocalDate
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

// Replaces the reference data shipped by the migrations with test-owned rows,
// so new real prices or CO2 factors never break these tests:
// sonnet has a past and a future version (relative to the mocked June 15), haiku only one.
private fun Jdbi.seedClaudeReferenceData() {
    useHandle<Exception> { handle ->
        handle.execute("DELETE FROM claude_prices")
        handle.execute(
            """
            INSERT INTO claude_prices (model, input_cent_per_million, output_cent_per_million, cache_read_cent_per_million,
                                       valid_from, cache_write_5m_cent_per_million, cache_write_1h_cent_per_million)
            VALUES ('sonnet', 300, 1500, 30, '2026-01-01', 375, 600),
                   ('sonnet', 200, 1000, 20, '2026-08-12', 250, 400),
                   ('haiku', 100, 500, 10, '2026-01-01', 125, 200)
            """.trimIndent(),
        )
        handle.execute("DELETE FROM co2_factors")
        handle.execute(
            """
            INSERT INTO co2_factors (model, input_factor, output_factor, cache_read_factor, cache_write_factor, valid_from)
            VALUES ('sonnet', 42, 840, 0.42, 52, '2026-01-01'),
                   ('haiku', 14, 280, 0.14, 17, '2026-01-01')
            """.trimIndent(),
        )
    }
}

class ClaudePricesIntegrationTest :
    FunSpec({
        val db = TestDatabase()
        val juneTimeFactory = fixedTimeFactory(LocalDate.of(2026, 6, 15))

        beforeSpec { db.start() }
        afterSpec { db.stop() }

        test("GET /api/prices/claude returns the currently valid price per model family") {
            val jdbi = db.buildJdbi()
            jdbi.seedClaudeReferenceData()
            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)

                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }
                val prices = jsonClient.get("/api/prices/claude").body<Map<String, ClaudePricesDto>>()

                prices.keys shouldBe setOf("haiku", "sonnet")
                // the 2026-08-12 sonnet version lies after the mocked "today" and must not win
                prices["sonnet"] shouldBe
                    ClaudePricesDto(
                        inputCentPerMillion = 300L,
                        outputCentPerMillion = 1500L,
                        cacheWrite5mCentPerMillion = 375L,
                        cacheWrite1hCentPerMillion = 600L,
                        cacheWriteCentPerMillion = 600L,
                        cacheReadCentPerMillion = 30L,
                    )
                prices["haiku"] shouldBe
                    ClaudePricesDto(
                        inputCentPerMillion = 100L,
                        outputCentPerMillion = 500L,
                        cacheWrite5mCentPerMillion = 125L,
                        cacheWrite1hCentPerMillion = 200L,
                        cacheWriteCentPerMillion = 200L,
                        cacheReadCentPerMillion = 10L,
                    )
            }
        }

        test("GET /api/prices/claude/history returns past and current versions, oldest first per family") {
            val jdbi = db.buildJdbi()
            jdbi.seedClaudeReferenceData()
            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)

                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }
                val history = jsonClient.get("/api/prices/claude/history").body<List<ClaudePriceVersionDto>>()

                history.filter { it.modelFamily == "sonnet" }.map { it.validFrom } shouldBe listOf("2026-01-01", "2026-08-12")
                history.first { it.modelFamily == "sonnet" && it.validFrom == "2026-08-12" }.prices.inputCentPerMillion shouldBe 200L
                history.map { it.modelFamily }.toSet() shouldBe setOf("haiku", "sonnet")
            }
        }

        test("GET /api/co2-factors/claude returns every factor version per family") {
            val jdbi = db.buildJdbi()
            jdbi.seedClaudeReferenceData()
            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)

                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }
                val factors = jsonClient.get("/api/co2-factors/claude").body<List<Co2FactorVersionDto>>()

                factors.first { it.modelFamily == "sonnet" } shouldBe
                    Co2FactorVersionDto(
                        modelFamily = "sonnet",
                        validFrom = "2026-01-01",
                        factors =
                            Co2FactorsDto(
                                inputGramCo2PerMillion = 42.0,
                                outputGramCo2PerMillion = 840.0,
                                cacheReadGramCo2PerMillion = 0.42,
                                cacheWriteGramCo2PerMillion = 52.0,
                            ),
                    )
                factors.map { it.modelFamily }.toSet() shouldBe setOf("haiku", "sonnet")
            }
        }
    })
