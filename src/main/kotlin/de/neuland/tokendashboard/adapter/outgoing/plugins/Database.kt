package de.neuland.tokendashboard.adapter.outgoing.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.ApplicationEnvironment
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import java.time.ZoneId

const val DEFAULT_DATABASE_PASSWORD = "postgres"

fun requireNonDefaultPassword(
    password: String,
    allowDefaultCredentials: Boolean,
) {
    require(allowDefaultCredentials || password != DEFAULT_DATABASE_PASSWORD) {
        "Refusing to start with the default database password. Set DATABASE_PASSWORD " +
            "(or ALLOW_DEFAULT_DB_CREDENTIALS=true for local development)."
    }
}

fun requireValidTimeZone(timeZone: String): String {
    require(runCatching { ZoneId.of(timeZone) }.isSuccess) {
        "database.reportingTimeZone (REPORTING_TIME_ZONE) must be a valid IANA time zone id, but was '$timeZone'"
    }
    return timeZone
}

fun configureDatabase(config: ApplicationEnvironment): Jdbi {
    val password = config.config.property("database.password").getString()
    val allowDefaultCredentials = config.config.property("database.allowDefaultCredentials").getString() == "true"
    requireNonDefaultPassword(password, allowDefaultCredentials)
    val reportingTimeZone = requireValidTimeZone(config.config.property("database.reportingTimeZone").getString())

    val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = config.config.property("database.url").getString()
            username = config.config.property("database.user").getString()
            this.password = password
            driverClassName = "org.postgresql.Driver"
            // Governs how `timestamp::date` and `date_trunc('week', ...)` bucket rows for reads.
            connectionInitSql = "SET TIME ZONE '$reportingTimeZone'"
        }
    val dataSource = HikariDataSource(hikariConfig)

    Flyway
        .configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    return Jdbi
        .create(dataSource)
        .installPlugin(KotlinPlugin())
        .installPlugin(KotlinSqlObjectPlugin())
        .installPlugin(PostgresPlugin())
}
