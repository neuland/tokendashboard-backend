package de.neuland.tokendashboard.adapter.outgoing.repository

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.time.LocalDate

interface UserRepository {
    @SqlUpdate(
        """
        INSERT INTO users (id, first_data_sent, last_data_sent, provider, plugin_version)
        VALUES (:id, :firstDataSent, :lastDataSent, :provider, :pluginVersion)
        ON CONFLICT (id, provider) DO UPDATE SET last_data_sent = :lastDataSent, plugin_version = :pluginVersion
        """,
    )
    fun upsert(
        @Bind("id") id: String,
        @Bind("firstDataSent") firstDataSent: LocalDate,
        @Bind("lastDataSent") lastDataSent: LocalDate,
        @Bind("provider") provider: String,
        @Bind("pluginVersion") pluginVersion: String,
    )

    @SqlQuery(
        """
        SELECT count(*)
        FROM users
        WHERE last_data_sent > :from
        AND provider = :provider
    """,
    )
    fun countActiveUsersFrom(
        @Bind("from") from: LocalDate,
        @Bind("provider") provider: String,
    ): Int

    @SqlQuery(
        """
        SELECT gs.day::date AS date, COUNT(u.id) AS count
        FROM generate_series(CAST(:from AS date), CAST(:to AS date), interval '1 day') AS gs(day)
        LEFT JOIN users u
            ON u.provider = :provider
            AND u.first_data_sent <= gs.day
            AND gs.day < u.last_data_sent + interval '4 weeks'
        GROUP BY gs.day
        ORDER BY gs.day
    """,
    )
    @RegisterKotlinMapper(PluginInstallationRow::class)
    fun countPluginInstallationsByDay(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
        @Bind("provider") provider: String,
    ): List<PluginInstallationRow>

    @SqlQuery(
        """
        SELECT date_trunc('week', gs.day::date)::date AS date, COUNT(DISTINCT u.id) AS count
        FROM generate_series(CAST(:from AS date), CAST(:to AS date), interval '1 day') AS gs(day)
        LEFT JOIN users u
            ON u.provider = :provider
            AND u.first_data_sent <= gs.day
            AND gs.day < u.last_data_sent + interval '4 weeks'
        GROUP BY date_trunc('week', gs.day::date)
        ORDER BY date_trunc('week', gs.day::date)
    """,
    )
    @RegisterKotlinMapper(PluginInstallationRow::class)
    fun countPluginInstallationsByWeek(
        @Bind("from") from: LocalDate,
        @Bind("to") to: LocalDate,
        @Bind("provider") provider: String,
    ): List<PluginInstallationRow>
}
