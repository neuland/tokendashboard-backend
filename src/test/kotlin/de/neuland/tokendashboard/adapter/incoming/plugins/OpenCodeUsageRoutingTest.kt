package de.neuland.tokendashboard.adapter.incoming.plugins

import arrow.core.toNonEmptyListOrNull
import de.neuland.tokendashboard.adapter.incoming.rest.MAX_REQUEST_BODY_BYTES
import de.neuland.tokendashboard.application.port.incoming.IngestOpenCodeUsagePort
import de.neuland.tokendashboard.application.port.incoming.QueryAllUsagePort
import de.neuland.tokendashboard.application.port.incoming.QueryProviderUsagePort
import de.neuland.tokendashboard.application.port.incoming.QueryProviderUsageSeriesPort
import de.neuland.tokendashboard.domain.factories.TimeFactory
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.ProviderPluginUsage
import de.neuland.tokendashboard.domain.model.ProviderUsageDetail
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UsageBucket
import de.neuland.tokendashboard.domain.model.UsageResult
import de.neuland.tokendashboard.domain.model.UsageSeries
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
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.koin.dsl.module
import java.time.LocalDate

class OpenCodeUsageRoutingTest :
    FunSpec({
        val today = Day(LocalDate.of(2026, 6, 15))
        val firstOfMonth = Day(LocalDate.of(2026, 6, 1))

        val timeFactory = fixedTimeFactory(today.value)

        val providerUsageQueryService = mockk<QueryProviderUsagePort>()
        val providerUsageSeriesQueryService = mockk<QueryProviderUsageSeriesPort>()
        val allUsageQueryService = mockk<QueryAllUsagePort>()
        val openCodeIngestService = mockk<IngestOpenCodeUsagePort>(relaxed = true)

        fun emptyOverview(range: DayRange) =
            ProviderPluginUsage(
                usage =
                    ProviderUsageDetail(
                        provider = OPENCODE,
                        range = range,
                        modelUsages = UsageResult.NoUsage,
                    ),
                activeUsersLast4Weeks = UserCount(0),
            )

        fun overviewWithModel(range: DayRange): ProviderPluginUsage =
            ProviderPluginUsage(
                usage =
                    ProviderUsageDetail(
                        provider = OPENCODE,
                        range = range,
                        modelUsages =
                            UsageResult.HasUsage(
                                listOf(
                                    ModelUsage(
                                        model = ModelName.restore("sonnet"),
                                        tokens =
                                            TokenCounts(
                                                inputTokens = TokenCount(100),
                                                outputTokens = TokenCount(200),
                                                cacheWriteTokens = TokenCount(10),
                                                cacheReadTokens = TokenCount(20),
                                            ),
                                        costUsdCent = DollarCent(50),
                                        estimatedCO2 = Co2Gram(5.0),
                                    ),
                                ).toNonEmptyListOrNull()!!,
                            ),
                    ),
                activeUsersLast4Weeks = UserCount(0),
            )

        fun emptySeries(range: DayRange) = UsageSeries(range, buckets = emptyList())

        fun seriesWithModel(range: DayRange): UsageSeries {
            val modelUsages =
                listOf(
                    ModelUsage(
                        model = ModelName.restore("sonnet"),
                        tokens =
                            TokenCounts(
                                inputTokens = TokenCount(100),
                                outputTokens = TokenCount(200),
                                cacheWriteTokens = TokenCount(10),
                                cacheReadTokens = TokenCount(20),
                            ),
                        costUsdCent = DollarCent(50),
                        estimatedCO2 = Co2Gram(5.0),
                    ),
                ).toNonEmptyListOrNull()!!
            return UsageSeries(
                range = range,
                buckets =
                    listOf(
                        UsageBucket(
                            date = range.to,
                            modelUsages = UsageResult.HasUsage(modelUsages),
                            pluginInstallations = UserCount(3),
                        ),
                    ),
            )
        }

        fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) =
            testRoutingApp(
                module {
                    single<TimeFactory> { timeFactory }
                    single<IngestOpenCodeUsagePort> { openCodeIngestService }
                    single<QueryProviderUsagePort> { providerUsageQueryService }
                    single<QueryProviderUsageSeriesPort> { providerUsageSeriesQueryService }
                    single<QueryAllUsagePort> { allUsageQueryService }
                },
                block = block,
            )

        test("GET /api/usage/opencode/v1 returns 200") {
            every {
                providerUsageQueryService.queryUsage(
                    OPENCODE,
                    any(),
                )
            } returns emptyOverview(DayRange.of(firstOfMonth, today))

            testApp {
                val response = client.get("/api/usage/opencode/v1?from=2026-06-01&to=2026-06-15")
                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("GET /api/usage/opencode/v1 without from returns 400") {
            testApp {
                val response = client.get("/api/usage/opencode/v1?to=2026-06-15")
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("GET /api/usage/opencode/v1 without to returns 400") {
            testApp {
                val response = client.get("/api/usage/opencode/v1?from=2026-06-01")
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("GET /api/usage/opencode/v1 with from and to params passes correct dates") {
            val rangeSlot = slot<DayRange>()
            val customFrom = Day(LocalDate.of(2026, 5, 1))
            val customTo = Day(LocalDate.of(2026, 5, 31))
            every { providerUsageQueryService.queryUsage(OPENCODE, capture(rangeSlot)) } returns
                emptyOverview(DayRange.of(customFrom, customTo))

            testApp {
                client.get("/api/usage/opencode/v1?from=2026-05-01&to=2026-05-31")
                rangeSlot.captured.from shouldBe customFrom
                rangeSlot.captured.to shouldBe customTo
            }
        }

        test("GET /api/usage/opencode/v1 response contains from and to as formatted dates") {
            every {
                providerUsageQueryService.queryUsage(OPENCODE, any())
            } returns emptyOverview(DayRange.of(firstOfMonth, today))

            testApp {
                val body = client.get("/api/usage/opencode/v1?from=2026-06-01&to=2026-06-15").bodyAsText()
                body shouldContain "2026-06-01"
                body shouldContain "2026-06-15"
            }
        }

        test("GET /api/usage/opencode/v1 response contains model data") {
            every { providerUsageQueryService.queryUsage(OPENCODE, any()) } returns
                overviewWithModel(DayRange.of(firstOfMonth, today))

            testApp {
                val body = client.get("/api/usage/opencode/v1?from=2026-06-01&to=2026-06-15").bodyAsText()
                body shouldContain "sonnet"
                body shouldContain "\"inputTokens\":100"
            }
        }

        test("GET /api/usage/opencode/v1 with malformed from param returns 400") {
            testApp {
                val response = client.get("/api/usage/opencode/v1?from=not-a-date&to=2026-06-15")
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("GET /api/usage/opencode/series/v1 returns 200") {
            every { providerUsageSeriesQueryService.querySeries(OPENCODE, any(), any()) } returns
                emptySeries(DayRange.of(firstOfMonth, today))

            testApp {
                val response = client.get("/api/usage/opencode/series/v1?from=2026-06-01&to=2026-06-15")
                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("GET /api/usage/opencode/series/v1 without from returns 400") {
            testApp {
                val response = client.get("/api/usage/opencode/series/v1?to=2026-06-15")
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("GET /api/usage/opencode/series/v1 without to returns 400") {
            testApp {
                val response = client.get("/api/usage/opencode/series/v1?from=2026-06-01")
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("GET /api/usage/opencode/series/v1 response contains history, buckets, model data and yyyy-MM-dd dates") {
            every { providerUsageSeriesQueryService.querySeries(OPENCODE, any(), any()) } returns
                seriesWithModel(DayRange.of(firstOfMonth, today))

            testApp {
                val body = client.get("/api/usage/opencode/series/v1?from=2026-06-01&to=2026-06-15").bodyAsText()
                body shouldContain "\"history\""
                body shouldContain "\"buckets\""
                body shouldContain "sonnet"
                body shouldContain "2026-06-15"
                body shouldContain "2026-06-01"
            }
        }

        val dayRange = DayRange.of(firstOfMonth, today)

        test("GET /api/usage/opencode/series/v1 without granularity defaults to day") {
            testApp {
                client.get("/api/usage/opencode/series/v1?from=2026-06-01&to=2026-06-15")

                verify { providerUsageSeriesQueryService.querySeries(OPENCODE, dayRange, Granularity.DAY) }
            }
        }

        test("GET /api/usage/opencode/series/v1 with granularity=week passes week") {
            testApp {
                client.get("/api/usage/opencode/series/v1?from=2026-06-01&to=2026-06-15&granularity=week")

                verify { providerUsageSeriesQueryService.querySeries(OPENCODE, dayRange, Granularity.WEEK) }
            }
        }

        test("GET /api/usage/opencode/series/v1 with granularity=day passes day") {
            testApp {
                client.get("/api/usage/opencode/series/v1?from=2026-06-01&to=2026-06-15&granularity=day")

                verify { providerUsageSeriesQueryService.querySeries(OPENCODE, dayRange, Granularity.DAY) }
            }
        }

        test("GET /api/usage/opencode/series/v1 with granularity=month returns 400") {
            testApp {
                val response =
                    client.get("/api/usage/opencode/series/v1?from=2026-06-01&to=2026-06-15&granularity=month")
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("GET /api/usage/opencode/series/v1 with range within 100 days (default granularity) returns 200") {
            every { providerUsageSeriesQueryService.querySeries(OPENCODE, any(), any()) } returns
                emptySeries(
                    DayRange.of(
                        Day(LocalDate.of(2026, 1, 1)),
                        Day(LocalDate.of(2026, 4, 11)),
                    ),
                )

            testApp {
                val response = client.get("/api/usage/opencode/series/v1?from=2026-01-01&to=2026-04-11")
                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("GET /api/usage/opencode/v1 with from after to returns 400") {
            testApp {
                val response = client.get("/api/usage/opencode/v1?from=2026-06-30&to=2026-06-01")
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("GET /api/usage/opencode/v1 with 5 years with 2 leap days returns OK") {
            testApp {
                val response = client.get("/api/usage/opencode/v1?from=2019-04-01&to=2024-03-31")
                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("GET /api/usage/opencode/v1 with a range beyond the maximum returns 400") {
            testApp {
                val response = client.get("/api/usage/opencode/v1?from=2019-04-01&to=2024-05-01")
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("GET /api/usage/opencode/series/v1 with a range beyond the maximum returns 400") {
            testApp {
                val response = client.get("/api/usage/opencode/series/v1?from=0001-01-01&to=9999-12-31")
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("POST /api/usage/ingest/opencode with a body beyond the limit returns 413") {
            testApp {
                val oversizedUserId = "u".repeat((MAX_REQUEST_BODY_BYTES + 1).toInt())

                val response =
                    client.post("/api/usage/ingest/opencode") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        setBody("""{"user_id":"$oversizedUserId","prompts":[]}""")
                    }

                response.status shouldBe HttpStatusCode.PayloadTooLarge
            }
        }
    })
