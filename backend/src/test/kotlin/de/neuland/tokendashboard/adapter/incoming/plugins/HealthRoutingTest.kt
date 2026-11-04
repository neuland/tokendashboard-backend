package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.adapter.outgoing.plugins.DatabaseHealthCheck
import de.neuland.tokendashboard.testsupport.testRoutingApp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import org.koin.dsl.module

class HealthRoutingTest :
    FunSpec({
        val databaseHealthCheck = mockk<DatabaseHealthCheck>()

        fun testApp(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) =
            testRoutingApp(
                module {
                    single { databaseHealthCheck }
                },
                block = block,
            )

        test("GET /health returns 200 when the database is reachable") {
            every { databaseHealthCheck.isReachable() } returns true

            testApp {
                val response = client.get("/health")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "ok"
            }
        }

        test("GET /health returns 503 when the database is unreachable") {
            every { databaseHealthCheck.isReachable() } returns false

            testApp {
                val response = client.get("/health")
                response.status shouldBe HttpStatusCode.ServiceUnavailable
                response.bodyAsText() shouldContain "error"
            }
        }

        // Fails with a Koin NoBeanDefFoundException if /health ever needs more than the health check itself.
        test("GET /health does not touch any other collaborator") {
            every { databaseHealthCheck.isReachable() } returns true

            testApp {
                client.get("/health").status shouldBe HttpStatusCode.OK
            }
        }
    })
