package de.neuland.tokendashboard.integrationtests

import de.neuland.tokendashboard.adapter.incoming.plugins.UsageSeriesResponseDto
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.testsupport.configureIntegrationTestApp
import de.neuland.tokendashboard.testsupport.fixedTimeFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
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
            .bind("provider", COPILOT)
            .bind("pluginVersion", "1.2.3")
            .execute()
        handle
            .createUpdate(insertUserSql)
            .bind("id", "userB")
            .bind("firstDataSent", LocalDate.of(2026, 6, 15))
            .bind("lastDataSent", LocalDate.of(2026, 6, 15))
            .bind("provider", COPILOT)
            .bind("pluginVersion", "1.2.3")
            .execute()
        handle
            .createUpdate(insertUserSql)
            .bind("id", "userC")
            .bind("firstDataSent", LocalDate.of(2026, 5, 1))
            .bind("lastDataSent", LocalDate.of(2026, 5, 1))
            .bind("provider", COPILOT)
            .bind("pluginVersion", "1.2.3")
            .execute()
    }
}

class CopilotUsageSeriesIntegrationTest :
    FunSpec({
        val db = TestDatabase()
        var testApp: TestApplication? = null
        lateinit var jsonClient: HttpClient
        val timeFactory = fixedTimeFactory(LocalDate.of(2026, 8, 1))

        beforeSpec {
            db.start()
            val jdbi = db.buildJdbi()

            val app = TestApplication { configureIntegrationTestApp(jdbi, timeFactory) }
            testApp = app
            app.start()
            jsonClient = app.createClient { install(ClientContentNegotiation) { json() } }

            postCopilotJune14(jsonClient).status shouldBe HttpStatusCode.Created
            postCopilotJune15First(jsonClient).status shouldBe HttpStatusCode.Created
            postCopilotJune15Second(jsonClient).status shouldBe HttpStatusCode.Created
            jdbi.seedPluginInstallationUsers()
        }

        afterSpec {
            testApp?.stop()
            db.stop()
        }

        test(
            "/api/usage/copilot/series/v1 returns sparse per-day buckets with model breakdown and pluginInstallations and day as default",
        ) {
            val response =
                jsonClient
                    .get("/api/usage/copilot/series/v1?from=2026-06-14&to=2026-06-16")
                    .body<UsageSeriesResponseDto>()

            response.history.provider.buckets shouldHaveSize 2

            val june14 = response.bucket("2026-06-14")
            val june15 = response.bucket("2026-06-15")

            june14.modelFamilies shouldHaveSize 2
            june15.modelFamilies shouldHaveSize 2

            val gpt14 = june14.modelFamilies.first { it.modelFamily == "gpt-5" }
            val gptMini14 = june14.modelFamilies.first { it.modelFamily == "gpt-5-mini" }

            gpt14.tokens.inputTokens shouldBe 100_000
            gpt14.tokens.outputTokens shouldBe 100_000
            gpt14.tokens.cacheReadTokens shouldBe 10_000_000
            gpt14.tokens.cacheWriteTokens shouldBe 1_000_000
            gpt14.tokens.costUsdCent shouldBe 12_012
            gpt14.tokens.estimatedCo2 shouldBe 0.0

            gptMini14.tokens.inputTokens shouldBe 100_000
            gptMini14.tokens.outputTokens shouldBe 100_000
            gptMini14.tokens.cacheReadTokens shouldBe 10_000_000
            gptMini14.tokens.cacheWriteTokens shouldBe 1_000_000
            gptMini14.tokens.costUsdCent shouldBe 8_008
            gptMini14.tokens.estimatedCo2 shouldBe 0.0

            june14.overallTotalTokens.inputTokens shouldBe 200_000
            june14.overallTotalTokens.outputTokens shouldBe 200_000
            june14.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
            june14.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
            june14.overallTotalTokens.costUsdCent shouldBe 20_020
            june14.overallTotalTokens.estimatedCo2 shouldBe 0.0

            val gpt15 = june15.modelFamilies.first { it.modelFamily.contains("gpt-5") }
            val gptMini15 = june15.modelFamilies.first { it.modelFamily.contains("gpt-5-mini") }

            gpt15.tokens.inputTokens shouldBe 100_000
            gpt15.tokens.outputTokens shouldBe 100_000
            gpt15.tokens.cacheReadTokens shouldBe 10_000_000
            gpt15.tokens.cacheWriteTokens shouldBe 1_000_000
            gpt15.tokens.costUsdCent shouldBe 12_012
            gpt15.tokens.estimatedCo2 shouldBe 0.0

            gptMini15.tokens.inputTokens shouldBe 100_000
            gptMini15.tokens.outputTokens shouldBe 100_000
            gptMini15.tokens.cacheReadTokens shouldBe 10_000_000
            gptMini15.tokens.cacheWriteTokens shouldBe 1_000_000
            gptMini15.tokens.costUsdCent shouldBe 8_008
            gptMini15.tokens.estimatedCo2 shouldBe 0.0

            june15.overallTotalTokens.inputTokens shouldBe 200_000
            june15.overallTotalTokens.outputTokens shouldBe 200_000
            june15.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
            june15.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
            june15.overallTotalTokens.costUsdCent shouldBe 20_020
            june15.overallTotalTokens.estimatedCo2 shouldBe 0.0

            june14.pluginInstallations shouldBe 1 // userA only
            june15.pluginInstallations shouldBe 2 // userA + userB
        }

        test("GET /api/usage/copilot/series/v1 returns sparse per-day buckets with day granularity paramater") {
            val response =
                jsonClient
                    .get("/api/usage/copilot/series/v1?from=2026-06-14&to=2026-06-16&granularity=day")
                    .body<UsageSeriesResponseDto>()

            response.history.provider.buckets shouldHaveSize 2

            val june14Resp = response.bucket("2026-06-14")
            val june15Resp = response.bucket("2026-06-15")

            june14Resp.overallTotalTokens.inputTokens shouldBe 200_000
            june14Resp.overallTotalTokens.costUsdCent shouldBe 20_020

            june15Resp.overallTotalTokens.inputTokens shouldBe 200_000
            june15Resp.overallTotalTokens.costUsdCent shouldBe 20_020

            june14Resp.pluginInstallations shouldBe 1 // userA only
            june15Resp.pluginInstallations shouldBe 2 // userA + userB
        }
    })
