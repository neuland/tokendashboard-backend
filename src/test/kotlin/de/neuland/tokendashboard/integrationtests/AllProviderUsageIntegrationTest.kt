package de.neuland.tokendashboard.integrationtests

import de.neuland.tokendashboard.adapter.incoming.plugins.AllProviderUsageDto
import de.neuland.tokendashboard.adapter.incoming.plugins.ProviderUsageDto
import de.neuland.tokendashboard.testsupport.configureIntegrationTestApp
import de.neuland.tokendashboard.testsupport.fixedTimeFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import java.time.LocalDate
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private fun AllProviderUsageDto.provider(name: String): ProviderUsageDto =
    providerUsages.entries
        .first {
            it.key.contains(name)
        }.value

class AllProviderUsageIntegrationTest :
    FunSpec({
        val db = TestDatabase()

        beforeSpec { db.start() }
        afterSpec { db.stop() }

        fun buildJdbi() = db.buildJdbi()

        test("POST ingest then GET aggregation roundtrip") {
            val jdbi = buildJdbi()
            val timeFactory = fixedTimeFactory(LocalDate.of(2026, 6, 15))

            testApplication {
                configureIntegrationTestApp(jdbi, timeFactory)

                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }

                val postClaudeJune14 = postClaudeJune14(client)
                postClaudeJune14.status shouldBe HttpStatusCode.Created

                val postClaudeJune15First = postClaudeJune15First(client)
                postClaudeJune15First.status shouldBe HttpStatusCode.Created

                val postClaudeJune15Second = postClaudeJune15Second(client)
                postClaudeJune15Second.status shouldBe HttpStatusCode.Created

                val postCopilotJune14 = postCopilotJune14(client)
                postCopilotJune14.status shouldBe HttpStatusCode.Created

                val postCopilotJune15First = postCopilotJune15First(client)
                postCopilotJune15First.status shouldBe HttpStatusCode.Created

                val postCopilotJune15Second = postCopilotJune15Second(client)
                postCopilotJune15Second.status shouldBe HttpStatusCode.Created

                val postOpenCodeJune14 = postOpenCodeJune14(client)
                postOpenCodeJune14.status shouldBe HttpStatusCode.Created

                val postOpenCodeJune15First = postOpenCodeJune15First(client)
                postOpenCodeJune15First.status shouldBe HttpStatusCode.Created

                val postOpenCodeJune15Second = postOpenCodeJune15Second(client)
                postOpenCodeJune15Second.status shouldBe HttpStatusCode.Created

                // when requesting only until 13
                val emptyBody =
                    jsonClient.get("/api/usage/all/v1?from=2026-06-10&to=2026-06-13").body<AllProviderUsageDto>()
                emptyBody.provider("CLAUDE").tokensInOut shouldBe 0
                emptyBody.provider("CLAUDE").totalCostUsdCent shouldBe 0
                emptyBody.provider("CLAUDE").totalEstimatedCo2 shouldBe 0.0
                emptyBody.provider("COPILOT").tokensInOut shouldBe 0
                emptyBody.provider("COPILOT").totalCostUsdCent shouldBe 0
                emptyBody.provider("COPILOT").totalEstimatedCo2 shouldBe 0.0
                emptyBody.provider("OPENCODE").tokensInOut shouldBe 0
                emptyBody.provider("OPENCODE").totalCostUsdCent shouldBe 0
                emptyBody.provider("OPENCODE").totalEstimatedCo2 shouldBe 0.0

                // when requesting june 14 + 15
                val with14 =
                    jsonClient.get("/api/usage/all/v1?from=2026-06-10&to=2026-06-15").body<AllProviderUsageDto>()
                with14.provider("CLAUDE").tokensInOut shouldBe 800_000
                with14.provider("CLAUDE").totalCostUsdCent shouldBe 2 * (SONNET_ONE_DAY + HAIKU_ONE_DAY)
                with14.provider("CLAUDE").totalEstimatedCo2 shouldBe (384.4 plusOrMinus 0.2)
                with14.provider("COPILOT").tokensInOut shouldBe 800_000
                with14.provider("COPILOT").totalCostUsdCent shouldBe 40_040
                with14.provider("COPILOT").totalEstimatedCo2 shouldBe 0.0
                with14.provider("OPENCODE").tokensInOut shouldBe 800_000
                with14.provider("OPENCODE").totalCostUsdCent shouldBe 13 // rounding
                with14.provider("OPENCODE").totalEstimatedCo2 shouldBe 0.0

                // when requesting only june 15
                val only15 =
                    jsonClient.get("/api/usage/all/v1?from=2026-06-15&to=2026-06-15").body<AllProviderUsageDto>()
                only15.provider("CLAUDE").tokensInOut shouldBe 400_000
                only15.provider("CLAUDE").totalCostUsdCent shouldBe SONNET_ONE_DAY + HAIKU_ONE_DAY
                only15.provider("CLAUDE").totalEstimatedCo2 shouldBe (192.2 plusOrMinus 0.2)
                only15.provider("COPILOT").tokensInOut shouldBe 400_000
                only15.provider("COPILOT").totalCostUsdCent shouldBe 20_020
                only15.provider("COPILOT").totalEstimatedCo2 shouldBe 0.0
                only15.provider("OPENCODE").tokensInOut shouldBe 400_000
                only15.provider("OPENCODE").totalCostUsdCent shouldBe 7
                only15.provider("OPENCODE").totalEstimatedCo2 shouldBe 0.0
            }
        }
    })
