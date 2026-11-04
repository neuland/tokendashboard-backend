package de.neuland.tokendashboard.integrationtests

import de.neuland.tokendashboard.adapter.outgoing.plugins.DatabaseHealthCheck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jdbi.v3.core.Jdbi

class DatabaseHealthCheckIntegrationTest :
    FunSpec({
        val db = TestDatabase()
        lateinit var jdbi: Jdbi

        beforeSpec {
            db.start()
            jdbi = db.buildJdbi()
        }
        afterSpec { db.stop() }

        test("isReachable returns true against a running database") {
            DatabaseHealthCheck(jdbi).isReachable() shouldBe true
        }
    })
