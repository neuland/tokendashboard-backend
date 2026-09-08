package de.neuland.tokendashboard.integrationtests

import de.neuland.tokendashboard.adapter.incoming.plugins.UsageSeriesResponseDto
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
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
            .bind("provider", OPENCODE)
            .bind("pluginVersion", "1.2.3")
            .execute()
        handle
            .createUpdate(insertUserSql)
            .bind("id", "userB")
            .bind("firstDataSent", LocalDate.of(2026, 6, 15))
            .bind("lastDataSent", LocalDate.of(2026, 6, 15))
            .bind("provider", OPENCODE)
            .bind("pluginVersion", "1.2.3")
            .execute()
        handle
            .createUpdate(insertUserSql)
            .bind("id", "userC")
            .bind("firstDataSent", LocalDate.of(2026, 5, 1))
            .bind("lastDataSent", LocalDate.of(2026, 5, 1))
            .bind("provider", OPENCODE)
            .bind("pluginVersion", "1.2.3")
            .execute()
    }
}

class OpenCodeUsageSeriesIntegrationTest :
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

            postOpenCodeJune14(jsonClient).status shouldBe HttpStatusCode.Created
            postOpenCodeJune15First(jsonClient).status shouldBe HttpStatusCode.Created
            postOpenCodeJune15Second(jsonClient).status shouldBe HttpStatusCode.Created
            jdbi.seedPluginInstallationUsers()
        }

        afterSpec {
            testApp?.stop()
            db.stop()
        }

        test(
            "/api/usage/opencode/series/v1 returns sparse per-day buckets with model breakdown and pluginInstallations and day as default",
        ) {
            val response =
                jsonClient
                    .get("/api/usage/opencode/series/v1?from=2026-06-14&to=2026-06-16")
                    .body<UsageSeriesResponseDto>()

            response.history.provider.buckets shouldHaveSize 2

            val june14 = response.bucket("2026-06-14")
            val june15 = response.bucket("2026-06-15")

            june14.modelFamilies shouldHaveSize 2
            june15.modelFamilies shouldHaveSize 2

            val deepseek14 = june14.modelFamilies.first { it.modelFamily == "deepseek-v4-pro" }
            val mimo14 = june14.modelFamilies.first { it.modelFamily == "mimo-v2.5-free" }

            deepseek14.tokens.inputTokens shouldBe 100_000
            deepseek14.tokens.outputTokens shouldBe 100_000
            deepseek14.tokens.cacheReadTokens shouldBe 10_000_000
            deepseek14.tokens.cacheWriteTokens shouldBe 1_000_000
            deepseek14.tokens.costUsdCent shouldBe 3
            deepseek14.tokens.estimatedCo2 shouldBe 0.0

            mimo14.tokens.inputTokens shouldBe 100_000
            mimo14.tokens.outputTokens shouldBe 100_000
            mimo14.tokens.cacheReadTokens shouldBe 10_000_000
            mimo14.tokens.cacheWriteTokens shouldBe 1_000_000
            mimo14.tokens.costUsdCent shouldBe 3
            mimo14.tokens.estimatedCo2 shouldBe 0.0

            june14.overallTotalTokens.inputTokens shouldBe 200_000
            june14.overallTotalTokens.outputTokens shouldBe 200_000
            june14.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
            june14.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
            june14.overallTotalTokens.costUsdCent shouldBe 6
            june14.overallTotalTokens.estimatedCo2 shouldBe 0.0

            val deepseek15 = june15.modelFamilies.first { it.modelFamily == "deepseek-v4-pro" }
            val mimo15 = june15.modelFamilies.first { it.modelFamily == "mimo-v2.5-free" }

            deepseek15.tokens.inputTokens shouldBe 100_000
            deepseek15.tokens.outputTokens shouldBe 100_000
            deepseek15.tokens.cacheReadTokens shouldBe 10_000_000
            deepseek15.tokens.cacheWriteTokens shouldBe 1_000_000
            deepseek15.tokens.costUsdCent shouldBe 4
            deepseek15.tokens.estimatedCo2 shouldBe 0.0

            mimo15.tokens.inputTokens shouldBe 100_000
            mimo15.tokens.outputTokens shouldBe 100_000
            mimo15.tokens.cacheReadTokens shouldBe 10_000_000
            mimo15.tokens.cacheWriteTokens shouldBe 1_000_000
            mimo15.tokens.costUsdCent shouldBe 2
            mimo15.tokens.estimatedCo2 shouldBe 0.0

            june15.overallTotalTokens.inputTokens shouldBe 200_000
            june15.overallTotalTokens.outputTokens shouldBe 200_000
            june15.overallTotalTokens.cacheReadTokens shouldBe 20_000_000
            june15.overallTotalTokens.cacheWriteTokens shouldBe 2_000_000
            june15.overallTotalTokens.costUsdCent shouldBe 6
            june15.overallTotalTokens.estimatedCo2 shouldBe 0.0

            june14.pluginInstallations shouldBe 1 // userA only
            june15.pluginInstallations shouldBe 2 // userA + userB
        }

        test("GET /api/usage/opencode/series/v1 returns sparse per-day buckets with day granularity paramater") {
            val response =
                jsonClient
                    .get("/api/usage/opencode/series/v1?from=2026-06-14&to=2026-06-16&granularity=day")
                    .body<UsageSeriesResponseDto>()

            response.history.provider.buckets shouldHaveSize 2

            val june14Resp = response.bucket("2026-06-14")
            val june15Resp = response.bucket("2026-06-15")

            june14Resp.overallTotalTokens.inputTokens shouldBe 200_000
            june14Resp.overallTotalTokens.costUsdCent shouldBe 6

            june15Resp.overallTotalTokens.inputTokens shouldBe 200_000
            june15Resp.overallTotalTokens.costUsdCent shouldBe 6

            june14Resp.pluginInstallations shouldBe 1 // userA only
            june15Resp.pluginInstallations shouldBe 2 // userA + userB
        }
    })
