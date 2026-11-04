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

    val jdbi =
        Jdbi
            .create(dataSource)
            .installPlugin(KotlinPlugin())
            .installPlugin(KotlinSqlObjectPlugin())
            .installPlugin(PostgresPlugin())
    return applyReportingTimeZone(jdbi, reportingTimeZone)
}

private val USAGE_TABLES_GROUPED_BY_DAY_AND_MODEL =
    listOf(
        "claude_usage_records" to "claude_usage_day_model",
        "copilot_usage_records" to "copilot_usage_day_model",
        "open_code_usage_records" to "open_code_usage_day_model",
    )

/**
 * Makes the reporting time zone available to queries as the SQL literal `<reportingTimeZone>` and
 * (re)creates the statistics that let Postgres estimate the number of day/model groups correctly.
 * Without them it assumes one group per row and sorts the whole range instead of hash-aggregating.
 * Statistics only match a literal expression, so they are rebuilt on every start for the configured zone.
 */
fun applyReportingTimeZone(
    jdbi: Jdbi,
    reportingTimeZone: String,
): Jdbi {
    val zoneLiteral = "'${requireValidTimeZone(reportingTimeZone)}'"
    jdbi.useTransaction<Exception> { handle ->
        USAGE_TABLES_GROUPED_BY_DAY_AND_MODEL.forEach { (table, statistics) ->
            handle.execute("DROP STATISTICS IF EXISTS $statistics")
            handle.execute(
                """
                CREATE STATISTICS $statistics (ndistinct)
                    ON ((timestamp AT TIME ZONE $zoneLiteral)::date), model
                    FROM $table
                """,
            )
            handle.execute("ANALYZE $table")
        }
    }
    return jdbi.define("reportingTimeZone", zoneLiteral)
}
