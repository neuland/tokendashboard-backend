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
        }.firstOrNull { it.model.contains(name) }

private fun ProviderPluginUsageDto.family(name: String) = usage.provider.modelFamilies.first { it.modelFamily.contains(name) }

class OpenCodeUsageIntegrationTest :
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

                val postResponse14June = postOpenCodeJune14(jsonClient)
                postResponse14June.status shouldBe HttpStatusCode.Created
                // send data twice
                val postResponse14JuneSecondTime = postOpenCodeJune14(jsonClient)
                // silently accepted, but never stored
                postResponse14JuneSecondTime.status shouldBe HttpStatusCode.Created

                val postResponse15June1 = postOpenCodeJune15First(jsonClient)
                postResponse15June1.status shouldBe HttpStatusCode.Created

                val postResponse15June2 = postOpenCodeJune15Second(jsonClient)
                postResponse15June2.status shouldBe HttpStatusCode.Created

                val postEmpty = postOpenCodeEmptyQwen(jsonClient)
                postEmpty.status shouldBe HttpStatusCode.Created

                // when requesting only until 13
                val emptyBody =
                    jsonClient.get("/api/usage/opencode/v1?from=2026-06-10&to=2026-06-13").body<ProviderPluginUsageDto>()
                emptyBody.usage.provider.modelFamilies
                    .shouldBeEmpty()
                emptyBody.activeUsersLast4Weeks shouldBe 3 // users are always returned for the last 4 weeks

                // when requesting june 14 + 15
                val with14 =
                    jsonClient.get("/api/usage/opencode/v1?from=2026-06-10&to=2026-06-15").body<ProviderPluginUsageDto>()

                val deepseekWith14 = with14.model("deepseek-v4-pro")!!
                deepseekWith14.tokens.inputTokens shouldBe 200_000
                deepseekWith14.tokens.outputTokens shouldBe 200_000
                deepseekWith14.tokens.cacheReadTokens shouldBe 20_000_000
                deepseekWith14.tokens.cacheWriteTokens shouldBe 2_000_000
                deepseekWith14.tokens.costUsdCent shouldBe 8
                deepseekWith14.tokens.estimatedCo2 shouldBe (0.0 plusOrMinus 0.2)
                deepseekWith14.model shouldBe "deepseek-v4-pro"

                val mimoWith14 = with14.model("mimo-v2.5-free")!!
                mimoWith14.tokens.inputTokens shouldBe 200_000
                mimoWith14.tokens.outputTokens shouldBe 200_000
                mimoWith14.tokens.cacheReadTokens shouldBe 20_000_000
                mimoWith14.tokens.cacheWriteTokens shouldBe 2_000_000
                mimoWith14.tokens.costUsdCent shouldBe 6
                mimoWith14.tokens.estimatedCo2 shouldBe (0.0 plusOrMinus 0.2)
                mimoWith14.model shouldBe "mimo-v2.5-free"

                with14.model("qwen") shouldBe null

                with14.usage.provider.overallTotalTokens!!
                    .inputTokens shouldBe 400_000
                with14.usage.provider.overallTotalTokens.outputTokens shouldBe 400_000
                with14.usage.provider.overallTotalTokens.cacheReadTokens shouldBe 40_000_000
                with14.usage.provider.overallTotalTokens.cacheWriteTokens shouldBe 4_000_000
                with14.usage.provider.overallTotalTokens.costUsdCent shouldBe 14
                with14.usage.provider.overallTotalTokens.estimatedCo2 shouldBe (0.0 plusOrMinus 0.2)
                with14.activeUsersLast4Weeks shouldBe 3

                // when requesting only june 15
                val only15 =
                    jsonClient.get("/api/usage/opencode/v1?from=2026-06-15&to=2026-06-15").body<ProviderPluginUsageDto>()

                val deepseekOnly15 = only15.model("deepseek-v4-pro")!!
                deepseekOnly15.tokens.inputTokens shouldBe 100_000
                deepseekOnly15.tokens.outputTokens shouldBe 100_000
                deepseekOnly15.tokens.cacheReadTokens shouldBe 10_000_000
                deepseekOnly15.tokens.cacheWriteTokens shouldBe 1_000_000
                deepseekOnly15.tokens.costUsdCent shouldBe 4
                deepseekOnly15.tokens.estimatedCo2 shouldBe (0.0 plusOrMinus 0.2)
                only15.family("deepseek-v4-pro") shouldNotBe null

                val mimoOnly15 = only15.model("mimo-v2.5-free")!!
                mimoOnly15.tokens.inputTokens shouldBe 100_000
                mimoOnly15.tokens.outputTokens shouldBe 100_000
                mimoOnly15.tokens.cacheReadTokens shouldBe 10_000_000
                mimoOnly15.tokens.cacheWriteTokens shouldBe 1_000_000
                mimoOnly15.tokens.costUsdCent shouldBe 2
                mimoOnly15.tokens.estimatedCo2 shouldBe (0.0 plusOrMinus 0.2)
                only15.family("mimo-v2.5-free") shouldNotBe null

                only15.model("qwen") shouldBe null

                only15.usage.provider.overallTotalTokens!!
                    .inputTokens shouldBe 200_000
                only15.usage.provider.overallTotalTokens.outputTokens shouldBe 200_000
                only15.usage.provider.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
                only15.usage.provider.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
                only15.usage.provider.overallTotalTokens.costUsdCent shouldBe 6
                only15.usage.provider.overallTotalTokens.estimatedCo2 shouldBe (0.0 plusOrMinus 0.2)
                only15.activeUsersLast4Weeks shouldBe 3 // users are always returned for the last 4 weeks

                pluginVersionFor(jdbi, "session-1") shouldBe "1.2.3"
            }
        }

        test("plugin_version is persisted when present in the payload") {
            val jdbi = db.buildJdbi()

            testApplication {
                configureIntegrationTestApp(jdbi, juneTimeFactory)
                val jsonClient = createClient { install(ClientContentNegotiation) { json() } }

                postOpenCodeJune14(jsonClient).status shouldBe HttpStatusCode.Created

                pluginVersionFor(jdbi, "session-1") shouldBe "1.2.3"
            }
        }
    })

private fun pluginVersionFor(
    jdbi: Jdbi,
    sessionId: String,
): String? =
    jdbi.withHandle<String?, Exception> { handle ->
        handle
            .createQuery("SELECT plugin_version FROM open_code_usage_records WHERE session_id = :sessionId")
            .bind("sessionId", sessionId)
            .mapTo(String::class.java)
            .findFirst()
            .orElse(null)
    }
