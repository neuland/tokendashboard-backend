package de.neuland.tokendashboard.integrationtests

import de.neuland.tokendashboard.adapter.incoming.plugins.ProviderPluginUsageDto
import de.neuland.tokendashboard.testsupport.configureIntegrationTestApp
import de.neuland.tokendashboard.testsupport.fixedTimeFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.jdbi.v3.core.Jdbi
import java.time.LocalDate
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private fun ProviderPluginUsageDto.model(name: String) =
    usage.provider.modelFamilies
        .flatMap {
            it.models
        }.first { it.model.contains(name) }

private fun ProviderPluginUsageDto.family(name: String) = usage.provider.modelFamilies.first { it.modelFamily.contains(name) }

class CopilotUsageIntegrationTest :
    FunSpec({
        val db = TestDatabase()
        val juneTimeFactory = fixedTimeFactory(LocalDate.of(2026, 6, 15))

        beforeSpec { db.start() }
        afterSpec { db.stop() }

        test("POST ingest then GET aggregation roundtrip") {
            val jdbi = db.buildJdbi()

            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)
                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }

                val postResponse14June = postCopilotJune14(jsonClient)
                postResponse14June.status shouldBe HttpStatusCode.Created

                val postResponse15June1 = postCopilotJune15First(jsonClient)
                postResponse15June1.status shouldBe HttpStatusCode.Created

                val postResponse15June2 = postCopilotJune15Second(jsonClient)
                postResponse15June2.status shouldBe HttpStatusCode.Created

                // when requesting only until 13
                val emptyBody =
                    jsonClient.get("/api/usage/copilot/v1?from=2026-06-10&to=2026-06-13").body<ProviderPluginUsageDto>()
                emptyBody.usage.provider.modelFamilies
                    .shouldBeEmpty()
                emptyBody.activeUsersLast4Weeks shouldBe 3 // users are always returned for the last 4 weeks

                // when requesting june 14 + 15
                val with14 =
                    jsonClient.get("/api/usage/copilot/v1?from=2026-06-10&to=2026-06-15").body<ProviderPluginUsageDto>()

                val gpt = with14.model("gpt-5.4")
                gpt.tokens.inputTokens shouldBe 200_000
                gpt.tokens.outputTokens shouldBe 200_000
                gpt.tokens.cacheReadTokens shouldBe 20_000_000
                gpt.tokens.cacheWriteTokens shouldBe 2_000_000
                gpt.tokens.costUsdCent shouldBe 24_024
                gpt.tokens.estimatedCo2 shouldBe 0.0
                with14.family("gpt-5") shouldNotBe null

                val mini = with14.model("gpt-5.4-mini")
                mini.tokens.inputTokens shouldBe 200_000
                mini.tokens.outputTokens shouldBe 200_000
                mini.tokens.cacheReadTokens shouldBe 20_000_000
                mini.tokens.cacheWriteTokens shouldBe 2_000_000
                mini.tokens.costUsdCent shouldBe 16_016
                mini.tokens.estimatedCo2 shouldBe 0.0
                with14.family("gpt-5-mini") shouldNotBe null

                with14.usage.provider.overallTotalTokens!!
                    .inputTokens shouldBe 400_000
                with14.usage.provider.overallTotalTokens.outputTokens shouldBe 400_000
                with14.usage.provider.overallTotalTokens.cacheReadTokens shouldBe 40_000_000
                with14.usage.provider.overallTotalTokens.cacheWriteTokens shouldBe 4_000_000
                with14.usage.provider.overallTotalTokens.costUsdCent shouldBe 40_040
                with14.usage.provider.overallTotalTokens.estimatedCo2 shouldBe 0.0
                with14.activeUsersLast4Weeks shouldBe 3

                // when requesting only june 15
                val only15 =
                    jsonClient.get("/api/usage/copilot/v1?from=2026-06-15&to=2026-06-15").body<ProviderPluginUsageDto>()

                val gpt15 = only15.model("gpt-5.4")
                gpt15.tokens.inputTokens shouldBe 100_000
                gpt15.tokens.outputTokens shouldBe 100_000
                gpt15.tokens.cacheReadTokens shouldBe 10_000_000
                gpt15.tokens.cacheWriteTokens shouldBe 1_000_000
                gpt15.tokens.costUsdCent shouldBe 12_012
                gpt15.tokens.estimatedCo2 shouldBe 0.0

                val mini15 = only15.model("gpt-5.4-mini")
                mini15.tokens.inputTokens shouldBe 100_000
                mini15.tokens.outputTokens shouldBe 100_000
                mini15.tokens.cacheReadTokens shouldBe 10_000_000
                mini15.tokens.cacheWriteTokens shouldBe 1_000_000
                mini15.tokens.costUsdCent shouldBe 8_008
                mini15.tokens.estimatedCo2 shouldBe 0.0

                only15.usage.provider.overallTotalTokens!!
                    .inputTokens shouldBe 200_000
                only15.usage.provider.overallTotalTokens.outputTokens shouldBe 200_000
                only15.usage.provider.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
                only15.usage.provider.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
                only15.usage.provider.overallTotalTokens.costUsdCent shouldBe 20_020
                only15.usage.provider.overallTotalTokens.estimatedCo2 shouldBe 0.0
                only15.activeUsersLast4Weeks shouldBe 3 // users are always returned for the last 4 weeks
            }
        }

        test("POST ingest sonnet for copilot") {
            val jdbi = db.buildJdbi()

            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)
                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }

                val responseAnthrophicModels = postCopilotSonnetAndHaiku(jsonClient)
                responseAnthrophicModels.status shouldBe HttpStatusCode.Created
                val onlyAnthropic =
                    jsonClient.get("/api/usage/copilot/v1?from=2026-06-17&to=2026-06-17").body<ProviderPluginUsageDto>()

                val sonnet = onlyAnthropic.model("sonnet")
                sonnet.tokens.inputTokens shouldBe 6_000
                sonnet.tokens.outputTokens shouldBe 100_000
                sonnet.tokens.cacheReadTokens shouldBe 10_000_000
                sonnet.tokens.cacheWriteTokens shouldBe 1_000_000
                sonnet.tokens.costUsdCent shouldBe 6006
                sonnet.tokens.estimatedCo2 shouldBe (140.5 plusOrMinus 0.2)
                onlyAnthropic.family("sonnet") shouldNotBe null

                val haiku = onlyAnthropic.model("haiku")
                haiku.tokens.inputTokens shouldBe 600
                haiku.tokens.outputTokens shouldBe 10_000
                haiku.tokens.cacheReadTokens shouldBe 4_000_000
                haiku.tokens.cacheWriteTokens shouldBe 450_000
                haiku.tokens.costUsdCent shouldBe 4
                haiku.tokens.estimatedCo2 shouldBe (11.0 plusOrMinus 0.2)
            }
        }

        test("resubmitting the same sessions does not double-count usage") {
            val jdbi = db.buildJdbi()

            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)
                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }

                postCopilotJune14(jsonClient).status shouldBe HttpStatusCode.Created
                postCopilotJune14(jsonClient).status shouldBe HttpStatusCode.Created

                val result =
                    jsonClient.get("/api/usage/copilot/v1?from=2026-06-14&to=2026-06-14").body<ProviderPluginUsageDto>()
                result.usage.provider.overallTotalTokens!!.inputTokens shouldBe 200_000
                result.usage.provider.overallTotalTokens.outputTokens shouldBe 200_000
            }
        }

        test("plugin_version is persisted in user and usage records") {
            val jdbi = db.buildJdbi()

            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)
                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }

                postCopilotJune14(jsonClient).status shouldBe HttpStatusCode.Created

                pluginVersion(jdbi, "session-1") shouldBe "1.2.3"
            }
        }

        test("ingest refuses missing plugin_version") {
            val jdbi = db.buildJdbi()

            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)
                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }

                postCopilotWithoutPluginVersion(jsonClient).status shouldBe HttpStatusCode.BadRequest
            }
        }
    })

private fun pluginVersion(
    jdbi: Jdbi,
    sessionId: String,
): String? =
    jdbi.withHandle<String?, Exception> { handle ->
        handle
            .createQuery("SELECT plugin_version FROM copilot_usage_records WHERE session_id = :sessionId")
            .bind("sessionId", sessionId)
            .mapTo(String::class.java)
            .findFirst()
            .orElse(null)
    }
