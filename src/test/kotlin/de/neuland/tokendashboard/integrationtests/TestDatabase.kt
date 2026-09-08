package de.neuland.tokendashboard.integrationtests

import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer

class TestDatabase {
    private val container: PostgreSQLContainer<Nothing>? =
        if (System.getenv("POSTGRES_HOST") == null) PostgreSQLContainer("postgres:16-alpine") else null

    fun start() {
        container?.start()
    }

    fun stop() {
        container?.stop()
    }

    private val jdbcUrl: String
        get() =
            container?.jdbcUrl ?: run {
                val host = System.getenv("POSTGRES_HOST")!!
                val port = System.getenv("POSTGRES_PORT") ?: "5432"
                val db = System.getenv("POSTGRES_DB") ?: "testdb"
                "jdbc:postgresql://$host:$port/$db"
            }
    private val username: String get() = container?.username ?: System.getenv("POSTGRES_USER") ?: "test"
    private val password: String get() = container?.password ?: System.getenv("POSTGRES_PASSWORD") ?: "test"

    fun buildJdbi(): Jdbi {
        val ds =
            PGSimpleDataSource().apply {
                setURL(jdbcUrl)
                user = username
                password = this@TestDatabase.password
            }
        val flyway =
            Flyway
                .configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
        flyway.clean()
        flyway.migrate()
        return Jdbi
            .create(ds)
            .installPlugin(KotlinPlugin())
            .installPlugin(KotlinSqlObjectPlugin())
            .installPlugin(PostgresPlugin())
    }
}
