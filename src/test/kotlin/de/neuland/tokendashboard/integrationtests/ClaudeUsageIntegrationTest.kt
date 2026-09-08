package de.neuland.tokendashboard.integrationtests

import de.neuland.tokendashboard.adapter.incoming.plugins.ProviderPluginUsageDto
import de.neuland.tokendashboard.testsupport.configureIntegrationTestApp
import de.neuland.tokendashboard.testsupport.fixedTimeFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.jdbi.v3.core.Jdbi
import java.time.LocalDate
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private fun ProviderPluginUsageDto.family(name: String) = usage.provider.modelFamilies.first { it.modelFamily.contains(name) }

private fun ProviderPluginUsageDto.model(name: String) =
    usage.provider.modelFamilies
        .flatMap {
            it.models
        }.first { it.model.contains(name) }

class ClaudeUsageIntegrationTest :
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

                val postResponse14June = postClaudeJune14(jsonClient)
                postResponse14June.status shouldBe HttpStatusCode.Created

                val postResponse15June1 = postClaudeJune15First(jsonClient)
                postResponse15June1.status shouldBe HttpStatusCode.Created

                val postResponse15June2 = postClaudeJune15Second(jsonClient)
                postResponse15June2.status shouldBe HttpStatusCode.Created

                // when requesting only until 13
                val emptyBody =
                    jsonClient.get("/api/usage/claude/v1?from=2026-06-10&to=2026-06-13").body<ProviderPluginUsageDto>()
                emptyBody.usage.provider.modelFamilies
                    .shouldBeEmpty()
                emptyBody.activeUsersLast4Weeks shouldBe 3 // users are always returned for the last 4 weeks

                // when requesting june 14 + 15
                val with14 =
                    jsonClient.get("/api/usage/claude/v1?from=2026-06-10&to=2026-06-15").body<ProviderPluginUsageDto>()

                val sonnet = with14.family("sonnet")
                sonnet.tokens.inputTokens shouldBe 200_000
                sonnet.tokens.outputTokens shouldBe 200_000
                sonnet.tokens.cacheReadTokens shouldBe 20_000_000
                sonnet.tokens.cacheWriteTokens shouldBe 2_000_000
                sonnet.tokens.costUsdCent shouldBe 2 * SONNET_ONE_DAY
                sonnet.tokens.estimatedCo2 shouldBe (288.8 plusOrMinus 0.2)
                sonnet
                    .models
                    .map { it.model }
                    .size shouldBe 1
                sonnet.models.map { it.model }[0] shouldBe "sonnet-4.6"
                sonnet.modelFamily shouldBe "sonnet"

                val haiku = with14.family("haiku")
                haiku.tokens.inputTokens shouldBe 200_000
                haiku.tokens.outputTokens shouldBe 200_000
                haiku.tokens.cacheReadTokens shouldBe 20_000_000
                haiku.tokens.cacheWriteTokens shouldBe 2_000_000
                haiku.tokens.costUsdCent shouldBe 2 * HAIKU_ONE_DAY
                sonnet
                    .models
                    .map { it.model }
                    .size shouldBe 1
                haiku.models.map { it.model }[0] shouldBe "haiku-4.5"
                haiku.modelFamily shouldBe "haiku"
                haiku.tokens.estimatedCo2 shouldBe (95.6 plusOrMinus 0.2)

                with14.usage.provider.overallTotalTokens!!
                    .inputTokens shouldBe 400_000
                with14.usage.provider.overallTotalTokens.outputTokens shouldBe 400_000
                with14.usage.provider.overallTotalTokens.cacheReadTokens shouldBe 40_000_000
                with14.usage.provider.overallTotalTokens.cacheWriteTokens shouldBe 4_000_000
                with14.usage.provider.overallTotalTokens.costUsdCent shouldBe 2 * (SONNET_ONE_DAY + HAIKU_ONE_DAY)
                with14.usage.provider.overallTotalTokens.estimatedCo2 shouldBe (384.4 plusOrMinus 0.2)
                with14.activeUsersLast4Weeks shouldBe 3

                // when requesting only june 15
                val only15 =
                    jsonClient.get("/api/usage/claude/v1?from=2026-06-15&to=2026-06-15").body<ProviderPluginUsageDto>()

                val sonnet15 = only15.family("sonnet")
                sonnet15.tokens.inputTokens shouldBe 100_000
                sonnet15.tokens.outputTokens shouldBe 100_000
                sonnet15.tokens.cacheReadTokens shouldBe 10_000_000
                sonnet15.tokens.cacheWriteTokens shouldBe 1_000_000
                sonnet15.tokens.costUsdCent shouldBe SONNET_ONE_DAY
                sonnet15.tokens.estimatedCo2 shouldBe (144.4 plusOrMinus 0.2)

                val haiku15 = only15.family("haiku")
                haiku15.tokens.inputTokens shouldBe 100_000
                haiku15.tokens.outputTokens shouldBe 100_000
                haiku15.tokens.cacheReadTokens shouldBe 10_000_000
                haiku15.tokens.cacheWriteTokens shouldBe 1_000_000
                haiku15.tokens.costUsdCent shouldBe HAIKU_ONE_DAY
                haiku15.tokens.estimatedCo2 shouldBe (47.8 plusOrMinus 0.2)

                only15.usage.provider.overallTotalTokens!!
                    .inputTokens shouldBe 200_000
                only15.usage.provider.overallTotalTokens.outputTokens shouldBe 200_000
                only15.usage.provider.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
                only15.usage.provider.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
                only15.usage.provider.overallTotalTokens.costUsdCent shouldBe SONNET_ONE_DAY + HAIKU_ONE_DAY
                only15.usage.provider.overallTotalTokens.estimatedCo2 shouldBe (192.2 plusOrMinus 0.2)
                only15.activeUsersLast4Weeks shouldBe 3 // users are always returned for the last 4 weeks
            }
        }

        test("resubmitting the same prompts does not double-count usage") {
            val jdbi = db.buildJdbi()

            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)
                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }

                postClaudeJune14(jsonClient).status shouldBe HttpStatusCode.Created
                postClaudeJune14(jsonClient).status shouldBe HttpStatusCode.Created

                val result =
                    jsonClient.get("/api/usage/claude/v1?from=2026-06-14&to=2026-06-14").body<ProviderPluginUsageDto>()
                result.model("sonnet").tokens.inputTokens shouldBe 100_000
                result.model("haiku").tokens.inputTokens shouldBe 100_000
            }
        }

        test("plugin_version is persisted") {
            val jdbi = db.buildJdbi()

            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)
                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }

                postClaudeJune14(jsonClient).status shouldBe HttpStatusCode.Created

                pluginVersionFor(jdbi, "session-1") shouldBe "1.2.3"
            }
        }

        test("mixed report with legacy aggregate and 5m/1h split cache-write fields sums correctly without double counting") {
            val jdbi = db.buildJdbi()

            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)
                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }

                postClaudeMixedCacheWriteDurations(jsonClient).status shouldBe HttpStatusCode.Created

                // report has 5 prompts, all model claude-sonnet-4-6, all on 2026-06-16:
                // 2 prompts with split fields only (5m=40000, 1h=5445 each)
                // 3 prompts with the legacy flat aggregate only (cache_creation_input_tokens=45445 each)
                // undated bucket must only reflect the 3 legacy prompts, never double-counted with the split ones
                val result =
                    jsonClient.get("/api/usage/claude/v1?from=2026-06-16&to=2026-06-16").body<ProviderPluginUsageDto>()

                val sonnet = result.family("sonnet")
                sonnet.tokens.inputTokens shouldBe 175 // 35 * 5
                sonnet.tokens.outputTokens shouldBe 77370 // 15474 * 5
                sonnet.tokens.cacheReadTokens shouldBe 3034995 // 606999 * 5
                // undated (136335 = 45445 * 3) + 5m (80000 = 40000 * 2) + 1h (10890 = 5445 * 2)
                sonnet.tokens.cacheWriteTokens shouldBe 227225
                // cost is rounded per prompt at ingestion, then summed — not rounded once on the aggregate:
                // 2 split prompts @ 60 cent each (59.69847 rounded) + 3 legacy prompts @ 69 cent each (58.473345 rounded)
                sonnet.tokens.costUsdCent shouldBe 327
                sonnet.tokens.estimatedCo2 shouldBe (78.09 plusOrMinus 0.05)
            }
        }
    })

private fun pluginVersionFor(
    jdbi: Jdbi,
    sessionId: String,
): String? =
    jdbi.withHandle<String?, Exception> { handle ->
        handle
            .createQuery("SELECT plugin_version FROM claude_usage_records WHERE session_id = :sessionId")
            .bind("sessionId", sessionId)
            .mapTo(String::class.java)
            .findFirst()
            .orElse(null)
    }
