package de.neuland.tokendashboard.integrationtests

import de.neuland.tokendashboard.adapter.incoming.plugins.ClaudePricesDto
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

// Replaces the prices shipped by the migrations with test-owned rows,
// so new real prices never break this test:
// sonnet has a past and a future version (relative to the mocked June 15), haiku only one.
private fun Jdbi.seedClaudePrices() {
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
            jdbi.seedClaudePrices()
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
    })
