package de.neuland.tokendashboard.integrationtests

import de.neuland.tokendashboard.adapter.incoming.plugins.UsageSeriesResponseDto
import de.neuland.tokendashboard.testsupport.configureIntegrationTestApp
import de.neuland.tokendashboard.testsupport.fixedTimeFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import org.jdbi.v3.core.Jdbi
import java.time.LocalDate
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private fun UsageSeriesResponseDto.bucket(date: String) = history.provider.buckets.first { it.date == date }

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
            .bind("pluginVersion", "unknown")
            .execute()
    }
}

class ClaudeUsageSeriesIntegrationTest :
    FunSpec({
        val db = TestDatabase()
        var testApp: TestApplication? = null
        lateinit var sharedJsonClient: HttpClient
        // today() is fixed well after both bucket dates (and their 4-week installation windows),
        // so the ingest-created upsert rows for user1/user2/user3 stay out of window for the fixed
        // June 2026 bucket dates used across this class - only the directly-seeded users A/B/C count.
        val timeFactory = fixedTimeFactory(LocalDate.of(2026, 8, 1))

        beforeSpec {
            db.start()
            val jdbi = db.buildJdbi()

            val app = TestApplication { configureIntegrationTestApp(jdbi, timeFactory) }
            testApp = app
            app.start()
            sharedJsonClient = app.createClient { install(ClientContentNegotiation) { json() } }

            postClaudeJune14(sharedJsonClient).status shouldBe HttpStatusCode.Created
            postClaudeJune15First(sharedJsonClient).status shouldBe HttpStatusCode.Created
            postClaudeJune15Second(sharedJsonClient).status shouldBe HttpStatusCode.Created
            jdbi.seedPluginInstallationUsers()
        }

        afterSpec {
            testApp?.stop()
            db.stop()
        }

        test(
            "/api/usage/claude/series/v1 returns sparse per-day buckets with model breakdown and pluginInstallations and day as default",
        ) {
            val response =
                sharedJsonClient
                    .get("/api/usage/claude/series/v1?from=2026-06-14&to=2026-06-16")
                    .body<UsageSeriesResponseDto>()

            // June 16 has no usage posted -> sparse, no bucket at all.
            response.history.provider.buckets shouldHaveSize 2

            val june14 = response.bucket("2026-06-14")
            val june15 = response.bucket("2026-06-15")

            june14.modelFamilies shouldHaveSize 2
            june15.modelFamilies shouldHaveSize 2

            val sonnet14 = june14.modelFamilies.first { it.modelFamily.contains("sonnet") }
            val haiku14 = june14.modelFamilies.first { it.modelFamily.contains("haiku") }
            sonnet14.tokens.inputTokens shouldBe 100_000
            sonnet14.tokens.outputTokens shouldBe 100_000
            sonnet14.tokens.cacheReadTokens shouldBe 10_000_000
            sonnet14.tokens.cacheWriteTokens shouldBe 1_000_000
            sonnet14.tokens.costUsdCent shouldBe SONNET_ONE_DAY
            sonnet14.tokens.estimatedCo2 shouldBe (144.4 plusOrMinus 0.5)
            haiku14.tokens.inputTokens shouldBe 100_000
            haiku14.tokens.outputTokens shouldBe 100_000
            haiku14.tokens.cacheReadTokens shouldBe 10_000_000
            haiku14.tokens.cacheWriteTokens shouldBe 1_000_000
            haiku14.tokens.costUsdCent shouldBe HAIKU_ONE_DAY
            haiku14.tokens.estimatedCo2 shouldBe (47.8 plusOrMinus 0.5)

            june14.overallTotalTokens.inputTokens shouldBe 200_000
            june14.overallTotalTokens.outputTokens shouldBe 200_000
            june14.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
            june14.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
            june14.overallTotalTokens.costUsdCent shouldBe SONNET_ONE_DAY + HAIKU_ONE_DAY
            june14.overallTotalTokens.estimatedCo2 shouldBe (192.2 plusOrMinus 0.5)

            val sonnet15 = june15.modelFamilies.first { it.modelFamily.contains("sonnet") }
            val haiku15 = june15.modelFamilies.first { it.modelFamily.contains("haiku") }
            sonnet15.tokens.inputTokens shouldBe 100_000
            sonnet15.tokens.outputTokens shouldBe 100_000
            sonnet15.tokens.cacheReadTokens shouldBe 10_000_000
            sonnet15.tokens.cacheWriteTokens shouldBe 1_000_000
            sonnet15.tokens.costUsdCent shouldBe SONNET_ONE_DAY
            sonnet15.tokens.estimatedCo2 shouldBe (144.4 plusOrMinus 0.5)
            haiku15.tokens.inputTokens shouldBe 100_000
            haiku15.tokens.outputTokens shouldBe 100_000
            haiku15.tokens.cacheReadTokens shouldBe 10_000_000
            haiku15.tokens.cacheWriteTokens shouldBe 1_000_000
            haiku15.tokens.costUsdCent shouldBe HAIKU_ONE_DAY
            haiku15.tokens.estimatedCo2 shouldBe (47.8 plusOrMinus 0.5)

            june15.overallTotalTokens.inputTokens shouldBe 200_000
            june15.overallTotalTokens.outputTokens shouldBe 200_000
            june15.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
            june15.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
            june15.overallTotalTokens.costUsdCent shouldBe SONNET_ONE_DAY + HAIKU_ONE_DAY
            june15.overallTotalTokens.estimatedCo2 shouldBe (192.2 plusOrMinus 0.5)

            // pluginInstallations: only the directly-seeded userA/userB/userC are counted -
            // the ingest-created user1/user2/user3 rows get first_data_sent = 2026-08-01 (today()),
            // which is after both bucket dates and therefore out of window.
            june14.pluginInstallations shouldBe 1 // userA only (userB's first_data_sent is 06-15, userC expired)
            june15.pluginInstallations shouldBe 2 // userA + userB (userC still expired)
        }

        test("GET /api/usage/claude/series/v1 returns sparse per-day buckets with day granularity paramater") {
            // June 14 (Sun) falls into the week starting 2026-06-08, June 15 (Mon) starts its own week,
            // so the two posted days collapse into 2 week buckets instead of 2 day buckets.
            val response =
                sharedJsonClient
                    .get("/api/usage/claude/series/v1?from=2026-06-14&to=2026-06-16&granularity=day")
                    .body<UsageSeriesResponseDto>()

            response.history.provider.buckets shouldHaveSize 2

            val june14 = response.bucket("2026-06-14")
            val june15 = response.bucket("2026-06-15")

            // june14 bucket only contains June 14's usage.
            june14.overallTotalTokens.inputTokens shouldBe 200_000
            june14.overallTotalTokens.costUsdCent shouldBe SONNET_ONE_DAY + HAIKU_ONE_DAY

            // june15 bucket contains both June 15 posts summed together.
            june15.overallTotalTokens.inputTokens shouldBe 200_000
            june15.overallTotalTokens.costUsdCent shouldBe SONNET_ONE_DAY + HAIKU_ONE_DAY

            june14.pluginInstallations shouldBe 1 // userA only
            june15.pluginInstallations shouldBe 2 // userA + userB
        }
    })
