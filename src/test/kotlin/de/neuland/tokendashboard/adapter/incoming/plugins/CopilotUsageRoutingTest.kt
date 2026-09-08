package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.application.port.incoming.IngestCopilotUsagePort
import de.neuland.tokendashboard.application.port.incoming.QueryAllUsagePort
import de.neuland.tokendashboard.application.port.incoming.QueryProviderUsagePort
import de.neuland.tokendashboard.application.port.incoming.QueryProviderUsageSeriesPort
import de.neuland.tokendashboard.domain.factories.TimeFactory
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.ProviderPluginUsage
import de.neuland.tokendashboard.domain.model.ProviderUsageDetail
import de.neuland.tokendashboard.domain.model.UsageResult
import de.neuland.tokendashboard.domain.model.UserCount
import de.neuland.tokendashboard.testsupport.fixedTimeFactory
import de.neuland.tokendashboard.testsupport.testRoutingApp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.koin.dsl.module
import java.time.LocalDate

class CopilotUsageRoutingTest :
    FunSpec({
        val today = Day(LocalDate.of(2026, 6, 15))
        val firstOfMonth = Day(LocalDate.of(2026, 6, 1))

        val timeFactory = fixedTimeFactory(today.value)

        val providerUsageQueryService = mockk<QueryProviderUsagePort>()
        val providerUsageSeriesQueryService = mockk<QueryProviderUsageSeriesPort>()
        val allUsageQueryService = mockk<QueryAllUsagePort>()
        val copilotIngestService = mockk<IngestCopilotUsagePort>(relaxed = true)

        fun emptyOverview(range: DayRange) =
            ProviderPluginUsage(
                usage =
                    ProviderUsageDetail(
                        provider = COPILOT,
                        range = range,
                        modelUsages = UsageResult.NoUsage,
                    ),
                activeUsersLast4Weeks = UserCount(0),
            )

        fun testApp(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) =
            testRoutingApp(
                module {
                    single<TimeFactory> { timeFactory }
                    single<IngestCopilotUsagePort> { copilotIngestService }
                    single<QueryProviderUsagePort> { providerUsageQueryService }
                    single<QueryProviderUsageSeriesPort> { providerUsageSeriesQueryService }
                    single<QueryAllUsagePort> { allUsageQueryService }
                },
                block = block,
            )

        test("POST /api/usage/ingest/copilot returns 201 for valid payload") {
            testApp {
                val response =
                    client.post("/api/usage/ingest/copilot") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        setBody(
                            """
                            {
                              "user_id": "58e50be1-e293-4c52-b962-3822ae2b4b72",
                              "plugin_version": "1.2.3",
                              "prompts": [
                                {
                                  "timestamp": "2026-06-04T15:55:06.079Z",
                                  "session_id": "09868499-4263-412f-9a5d-59b77235010c",
                                  "model": "gpt-5.4-mini",
                                  "usage": {
                                    "input_tokens": 34758,
                                    "output_tokens": 1637,
                                    "cache_read_tokens": 125440,
                                    "cache_write_tokens": 0,
                                    "reasoning_tokens": 967
                                  },
                                  "requests": 6,
                                  "total_nano_aiu": 4284300000,
                                  "total_premium_requests": 0.33
                                }
                              ]
                            }
                            """.trimIndent(),
                        )
                    }
                response.status shouldBe HttpStatusCode.Created
            }
        }

        test("POST /api/usage/ingest/copilot returns 400 for empty prompts") {
            testApp {
                val response =
                    client.post("/api/usage/ingest/copilot") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        setBody("""{"user_id":"user1","prompts":[]}""")
                    }
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("GET /api/usage/copilot/v1 returns 200") {
            every {
                providerUsageQueryService.queryUsage(COPILOT, any())
            } returns emptyOverview(DayRange.of(firstOfMonth, today))

            testApp {
                val response = client.get("/api/usage/copilot/v1?from=2026-06-01&to=2026-06-15")
                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("GET /api/usage/copilot/v1 response contains from and to") {
            every {
                providerUsageQueryService.queryUsage(COPILOT, any())
            } returns emptyOverview(DayRange.of(firstOfMonth, today))

            testApp {
                val body = client.get("/api/usage/copilot/v1?from=2026-06-01&to=2026-06-15").bodyAsText()
                body shouldContain "2026-06-01"
                body shouldContain "2026-06-15"
            }
        }

        test("GET /api/usage/copilot/v1 without from returns 400") {
            testApp {
                val response = client.get("/api/usage/copilot/v1?to=2026-06-15")
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("GET /api/usage/copilot/v1 without to returns 400") {
            testApp {
                val response = client.get("/api/usage/copilot/v1?from=2026-06-01")
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        val dayRange = DayRange.of(firstOfMonth, today)

        test("GET /api/usage/copilot/series/v1 without granularity defaults to day") {
            testApp {
                client.get("/api/usage/copilot/series/v1?from=2026-06-01&to=2026-06-15")

                verify { providerUsageSeriesQueryService.querySeries(COPILOT, dayRange, Granularity.DAY) }
            }
        }

        test("GET /api/usage/copilot/series/v1 with granularity=week passes week") {
            testApp {
                client.get("/api/usage/copilot/series/v1?from=2026-06-01&to=2026-06-15&granularity=week")

                verify { providerUsageSeriesQueryService.querySeries(COPILOT, dayRange, Granularity.WEEK) }
            }
        }

        test("GET /api/usage/copilot/series/v1 with granularity=day passes day") {
            testApp {
                client.get("/api/usage/copilot/series/v1?from=2026-06-01&to=2026-06-15&granularity=day")

                verify { providerUsageSeriesQueryService.querySeries(COPILOT, dayRange, Granularity.DAY) }
            }
        }
    })
