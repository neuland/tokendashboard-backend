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
import java.time.LocalDate
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class ClaudePricesIntegrationTest :
    FunSpec({
        val db = TestDatabase()
        val juneTimeFactory = fixedTimeFactory(LocalDate.of(2026, 6, 15))

        beforeSpec { db.start() }
        afterSpec { db.stop() }

        test("GET /api/prices/claude returns the currently valid price per model family") {
            val jdbi = db.buildJdbi()
            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)

                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }
                val prices = jsonClient.get("/api/prices/claude").body<Map<String, ClaudePricesDto>>()

                prices["sonnet"] shouldBe
                    ClaudePricesDto(
                        inputCentPerMillion = 300L,
                        outputCentPerMillion = 1500L,
                        cacheWrite5mCentPerMillion = 375L,
                        cacheWrite1hCentPerMillion = 600L,
                        cacheWriteCentPerMillion = 600,
                        cacheReadCentPerMillion = 30L,
                    )
                prices["opus"] shouldBe
                    ClaudePricesDto(
                        inputCentPerMillion = 500L,
                        outputCentPerMillion = 2500L,
                        cacheWrite5mCentPerMillion = 625L,
                        cacheWrite1hCentPerMillion = 1000L,
                        cacheWriteCentPerMillion = 1000L,
                        cacheReadCentPerMillion = 50L,
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
                prices["fable"] shouldBe
                    ClaudePricesDto(
                        inputCentPerMillion = 1000L,
                        outputCentPerMillion = 5000L,
                        cacheWrite5mCentPerMillion = 1250L,
                        cacheWrite1hCentPerMillion = 2000L,
                        cacheWriteCentPerMillion = 2000L,
                        cacheReadCentPerMillion = 100L,
                    )
            }
        }
    })
