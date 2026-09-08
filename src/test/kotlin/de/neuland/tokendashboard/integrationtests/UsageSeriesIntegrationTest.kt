package de.neuland.tokendashboard.integrationtests

import de.neuland.tokendashboard.adapter.incoming.plugins.AllProviderUsageSeriesResponseDto
import de.neuland.tokendashboard.adapter.incoming.plugins.AllUsageBucketDto
import de.neuland.tokendashboard.testsupport.configureIntegrationTestApp
import de.neuland.tokendashboard.testsupport.fixedTimeFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.jdbi.v3.core.Jdbi
import java.time.LocalDate
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private fun AllProviderUsageSeriesResponseDto.bucket(date: String) = history.buckets.find { bucketDto -> bucketDto.date == date }!!

private fun AllUsageBucketDto.provider(provider: String) = providers.get(provider)!!

private fun ApplicationTestBuilder.jsonClient(): HttpClient = createClient { install(ClientContentNegotiation) { json() } }

// Directly seed users to exercise the pluginInstallations window, bypassing upsert:
// A: in-window for both June 14 and June 15 buckets.
// B: first_data_sent == June 15 -> in-window for June 15 only (exercises the lower bound).
// C: window closes 2026-05-29 -> expired before both buckets (exercises the 4-week upper bound).
private fun Jdbi.seedPluginInstallationUsers() {
    val insertUserSql =
        """
        INSERT INTO users (id, first_data_sent, last_data_sent, provider, plugin_version)
        VALUES (:id, :firstDataSent, :lastDataSent, :provider, :pluginVersion)
        """.trimIndent()
    withHandle<Unit, Exception> { handle ->
        handle
            .createUpdate(insertUserSql)
            .bind("id", "userA")
            .bind("firstDataSent", LocalDate.of(2026, 6, 1))
            .bind("lastDataSent", LocalDate.of(2026, 6, 14))
            .bind("provider", "CLAUDE")
            .bind("pluginVersion", "1.2.3")
            .execute()
        handle
            .createUpdate(insertUserSql)
            .bind("id", "userB")
            .bind("firstDataSent", LocalDate.of(2026, 6, 15))
            .bind("lastDataSent", LocalDate.of(2026, 6, 15))
            .bind("provider", "CLAUDE")
            .bind("pluginVersion", "1.2.3")
            .execute()
        handle
            .createUpdate(insertUserSql)
            .bind("id", "userC")
            .bind("firstDataSent", LocalDate.of(2026, 5, 1))
            .bind("lastDataSent", LocalDate.of(2026, 5, 1))
            .bind("provider", "CLAUDE")
            .bind("pluginVersion", "1.2.3")
            .execute()
        handle
            .createUpdate(insertUserSql)
            .bind("id", "userA")
            .bind("firstDataSent", LocalDate.of(2026, 6, 1))
            .bind("lastDataSent", LocalDate.of(2026, 6, 14))
            .bind("provider", "COPILOT")
            .bind("pluginVersion", "1.2.3")
            .execute()
        handle
            .createUpdate(insertUserSql)
            .bind("id", "userB")
            .bind("firstDataSent", LocalDate.of(2026, 6, 15))
            .bind("lastDataSent", LocalDate.of(2026, 6, 15))
            .bind("provider", "COPILOT")
            .bind("pluginVersion", "1.2.3")
            .execute()
        handle
            .createUpdate(insertUserSql)
            .bind("id", "userC")
            .bind("firstDataSent", LocalDate.of(2026, 5, 1))
            .bind("lastDataSent", LocalDate.of(2026, 5, 1))
            .bind("provider", "COPILOT")
            .bind("pluginVersion", "1.2.3")
            .execute()
        handle
            .createUpdate(insertUserSql)
            .bind("id", "userB")
            .bind("firstDataSent", LocalDate.of(2026, 6, 15))
            .bind("lastDataSent", LocalDate.of(2026, 6, 15))
            .bind("provider", "OPENCODE")
            .bind("pluginVersion", "1.2.3")
            .execute()
        handle
            .createUpdate(insertUserSql)
            .bind("id", "userC")
            .bind("firstDataSent", LocalDate.of(2026, 5, 1))
            .bind("lastDataSent", LocalDate.of(2026, 5, 1))
            .bind("provider", "OPENCODE")
            .bind("pluginVersion", "1.2.3")
            .execute()
    }
}

class UsageSeriesIntegrationTest :
    FunSpec({
        val db = TestDatabase()

        beforeSpec { db.start() }
        afterSpec { db.stop() }

        test(
            "GET /api/usage/all/series/v1 returns sparse per-day buckets with model breakdown and pluginInstallations and day as default",
        ) {
            val jdbi = db.buildJdbi()
            // today() is fixed well after both bucket dates (and their 4-week installation windows),
            // so the ingest-created upsert rows for user1/user2/user3 stay out of window for the fixed
            // June 2026 bucket dates used across this class - only the directly-seeded users A/B/C count.
            val timeFactory = fixedTimeFactory(LocalDate.of(2026, 8, 1))

            testApplication {
                configureIntegrationTestApp(jdbi, timeFactory)
                val jsonClient = jsonClient()

                postClaudeJune14(client).status shouldBe HttpStatusCode.Created
                postClaudeJune15First(client).status shouldBe HttpStatusCode.Created
                postClaudeJune15Second(client).status shouldBe HttpStatusCode.Created

                postCopilotJune14(client).status shouldBe HttpStatusCode.Created
                postCopilotJune15First(client).status shouldBe HttpStatusCode.Created
                postCopilotJune15Second(client).status shouldBe HttpStatusCode.Created

                postOpenCodeJune15First(client).status shouldBe HttpStatusCode.Created
                postOpenCodeJune15Second(client).status shouldBe HttpStatusCode.Created

                jdbi.seedPluginInstallationUsers()

                val response =
                    jsonClient
                        .get("/api/usage/all/series/v1?from=2026-06-14&to=2026-06-16")
                        .body<AllProviderUsageSeriesResponseDto>()

                // June 16 has no usage posted -> sparse, no bucket at all.
                response.history.buckets shouldHaveSize 2

                val june14 = response.bucket("2026-06-14")
                val june15 = response.bucket("2026-06-15")
                val claudeJune14 = june14.provider("CLAUDE")
                val copilotJune14 = june14.provider("COPILOT")
                val claudeJune15 = june15.provider("CLAUDE")
                val copilotJune15 = june15.provider("COPILOT")
                val openCodeJune15 = june15.provider("OPENCODE")

                // june 14 only claude and copilot
                june14.providers shouldHaveSize 2
                // june 15 all providers
                june15.providers shouldHaveSize 3

                claudeJune14.overallTotalTokens.inputTokens shouldBe 200_000
                claudeJune14.overallTotalTokens.outputTokens shouldBe 200_000
                claudeJune14.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
                claudeJune14.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
                claudeJune14.pluginInstallations shouldBe 1

                claudeJune15.overallTotalTokens.inputTokens shouldBe 200_000
                claudeJune15.overallTotalTokens.outputTokens shouldBe 200_000
                claudeJune15.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
                claudeJune15.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
                claudeJune15.pluginInstallations shouldBe 2

                copilotJune14.overallTotalTokens.inputTokens shouldBe 200_000
                copilotJune14.overallTotalTokens.outputTokens shouldBe 200_000
                copilotJune14.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
                copilotJune14.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
                copilotJune14.pluginInstallations shouldBe 1

                copilotJune15.overallTotalTokens.inputTokens shouldBe 200_000
                copilotJune15.overallTotalTokens.outputTokens shouldBe 200_000
                copilotJune15.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
                copilotJune15.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
                copilotJune15.pluginInstallations shouldBe 2

                openCodeJune15.overallTotalTokens.inputTokens shouldBe 200_000
                openCodeJune15.overallTotalTokens.outputTokens shouldBe 200_000
                openCodeJune15.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
                openCodeJune15.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
                openCodeJune15.pluginInstallations shouldBe 1
            }
        }
    })
