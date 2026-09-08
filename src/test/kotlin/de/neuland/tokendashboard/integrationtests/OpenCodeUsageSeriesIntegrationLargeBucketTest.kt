package de.neuland.tokendashboard.integrationtests

import de.neuland.tokendashboard.adapter.incoming.plugins.UsageSeriesResponseDto
import de.neuland.tokendashboard.testsupport.configureIntegrationTestApp
import de.neuland.tokendashboard.testsupport.fixedTimeFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.jdbi.v3.core.Jdbi
import java.time.DayOfWeek
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
            .bind("provider", "OPENCODE")
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
            .bind("pluginVersion", "unknown")
            .execute()
    }
}

class OpenCodeUsageSeriesIntegrationLargeBucketTest :
    FunSpec({
        val db = TestDatabase()
        beforeSpec {
            db.start()
        }
        afterSpec {
            db.stop()
        }

        test("GET /api/usage/opencode/series/v1 sums fake usage data into per-week buckets across a 4-week range") {
            val jdbi = db.buildJdbi()
            val timeFactory = fixedTimeFactory(LocalDate.of(2026, 8, 1))

            val sql =
                object {}
                    .javaClass
                    .getResourceAsStream("/fill_usage_rows_opencode.sql")!!
                    .bufferedReader()
                    .use { it.readText() }
            jdbi.withHandle<Unit, Exception> { handle ->
                @Suppress("SqlSourceToSinkFlow")
                handle.createScript(sql).execute()
            }
            jdbi.seedPluginInstallationUsers()

            testApplication {
                configureIntegrationTestApp(jdbi, timeFactory)
                val jsonClient = createClient { this.install(ClientContentNegotiation) { json() } }

                // Range spans exactly 4 ISO weeks (Mon-Sun), offset so the first and last bucket are
                // partial weeks (missing Monday resp. only Monday) - see below for why that matters.
                val response =
                    jsonClient
                        .get("/api/usage/opencode/series/v1?from=2026-06-02&to=2026-06-29&granularity=week")
                        .body<UsageSeriesResponseDto>()

                response.history.provider.buckets shouldHaveSize 5
                response.history.provider.buckets.forEach { bucket ->
                    LocalDate.parse(bucket.date).dayOfWeek shouldBe DayOfWeek.MONDAY
                }

                val week01 = response.bucket("2026-06-01") // partial: 06-02..06-07, Monday missing
                val week08 = response.bucket("2026-06-08") // full week
                val week15 = response.bucket("2026-06-15") // full week
                val week22 = response.bucket("2026-06-22") // full week
                val week29 = response.bucket("2026-06-29") // partial: only 06-29 (Monday)

                // assure not zero
                week01.overallTotalTokens.inputTokens shouldBeGreaterThan 1

                // Fake data repeats identically every other week, so full weeks two apart match...
                week08.overallTotalTokens.inputTokens shouldBe week22.overallTotalTokens.inputTokens
                week08.overallTotalTokens.cacheReadTokens shouldBe week22.overallTotalTokens.cacheReadTokens

                // ...and the two partial weeks at the range edges together cover exactly one full week's
                // worth of weekdays (week01 = Tue-Sun, week29 = Mon only), matching the full week between them.
                val edgesSummed = week01.overallTotalTokens.inputTokens + week29.overallTotalTokens.inputTokens
                edgesSummed shouldBe week15.overallTotalTokens.inputTokens

                week08.pluginInstallations shouldBe 1 // userA only
                week15.pluginInstallations shouldBe 2 // userA + userB
            }
        }
    })
