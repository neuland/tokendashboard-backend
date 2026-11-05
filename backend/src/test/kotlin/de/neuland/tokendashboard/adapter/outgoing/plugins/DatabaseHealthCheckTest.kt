package de.neuland.tokendashboard.adapter.outgoing.plugins

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jdbi.v3.core.Jdbi

class DatabaseHealthCheckTest :
    FunSpec({
        test("isReachable returns false when the database is unreachable") {
            val unreachableJdbi =
                Jdbi.create("jdbc:postgresql://127.0.0.1:1/nonexistent?connectTimeout=1", "nobody", "nobody")

            DatabaseHealthCheck(unreachableJdbi).isReachable() shouldBe false
        }
    })
