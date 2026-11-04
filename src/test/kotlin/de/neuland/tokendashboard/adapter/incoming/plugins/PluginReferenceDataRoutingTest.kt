package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.application.port.incoming.QueryCo2FactorHistoryPort
import de.neuland.tokendashboard.application.port.incoming.QueryPriceHistoryPort
import de.neuland.tokendashboard.domain.model.CentPerMillionToken
import de.neuland.tokendashboard.domain.model.ClaudePriceVersion
import de.neuland.tokendashboard.domain.model.ClaudePriceVersions
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.Co2Factor
import de.neuland.tokendashboard.domain.model.Co2FactorVersion
import de.neuland.tokendashboard.domain.model.Co2FactorVersions
import de.neuland.tokendashboard.domain.model.Co2Factors
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily
import de.neuland.tokendashboard.testsupport.testRoutingApp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.dsl.module
import java.time.LocalDate

class PluginReferenceDataRoutingTest :
    FunSpec({
        val priceHistory = mockk<QueryPriceHistoryPort>()
        val co2FactorHistory = mockk<QueryCo2FactorHistoryPort>()

        fun testApp(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) =
            testRoutingApp(
                module {
                    single { priceHistory }
                    single { co2FactorHistory }
                },
                block = block,
            )

        fun prices(input: Long) =
            ClaudePrices(
                inputCentPerMillion = CentPerMillionToken(input),
                outputCentPerMillion = CentPerMillionToken(input * 5),
                cacheWrite5mCentPerMillion = CentPerMillionToken(input * 5 / 4),
                cacheWrite1hCentPerMillion = CentPerMillionToken(input * 2),
                cacheReadCentPerMillion = CentPerMillionToken(input / 10),
            )

        test("GET /api/prices/claude/history lists every version with family, date and prices") {
            every { priceHistory.queryPriceHistory() } returns
                ClaudePriceVersions(
                    listOf(
                        ClaudePriceVersion(ModelFamily("sonnet"), Day(LocalDate.of(2026, 1, 1)), prices(300)),
                        ClaudePriceVersion(ModelFamily("sonnet"), Day(LocalDate.of(2026, 8, 12)), prices(200)),
                    ),
                )

            testApp {
                val response = client.get("/api/prices/claude/history")
                val versions = Json.parseToJsonElement(response.bodyAsText()).jsonArray

                response.status shouldBe HttpStatusCode.OK
                versions.size shouldBe 2
                versions[1].jsonObject["modelFamily"]?.jsonPrimitive?.content shouldBe "sonnet"
                versions[1].jsonObject["validFrom"]?.jsonPrimitive?.content shouldBe "2026-08-12"
                versions[1]
                    .jsonObject["prices"]
                    ?.jsonObject
                    ?.get("inputCentPerMillion")
                    ?.jsonPrimitive
                    ?.content shouldBe "200"
            }
        }

        test("GET /api/co2-factors/claude lists every version with family, date and factors") {
            every { co2FactorHistory.queryCo2FactorHistory() } returns
                Co2FactorVersions(
                    listOf(
                        Co2FactorVersion(
                            ModelFamily("opus"),
                            Day(LocalDate.of(2026, 1, 1)),
                            Co2Factors(Co2Factor(70.0), Co2Factor(1400.0), Co2Factor(0.7), Co2Factor(87.0)),
                        ),
                    ),
                )

            testApp {
                val response = client.get("/api/co2-factors/claude")
                val version =
                    Json
                        .parseToJsonElement(response.bodyAsText())
                        .jsonArray
                        .single()
                        .jsonObject

                response.status shouldBe HttpStatusCode.OK
                version["modelFamily"]?.jsonPrimitive?.content shouldBe "opus"
                version["validFrom"]?.jsonPrimitive?.content shouldBe "2026-01-01"
                version["factors"]
                    ?.jsonObject
                    ?.get("outputGramCo2PerMillion")
                    ?.jsonPrimitive
                    ?.content shouldBe "1400.0"
            }
        }

        test("both routes answer with an empty list when nothing is stored") {
            every { priceHistory.queryPriceHistory() } returns ClaudePriceVersions(emptyList())
            every { co2FactorHistory.queryCo2FactorHistory() } returns Co2FactorVersions(emptyList())

            testApp {
                Json.parseToJsonElement(client.get("/api/prices/claude/history").bodyAsText()) shouldBe
                    JsonArray(
                        emptyList(),
                    )
                Json.parseToJsonElement(
                    client.get("/api/co2-factors/claude").bodyAsText(),
                ) shouldBe JsonArray(emptyList())
            }
        }
    })
