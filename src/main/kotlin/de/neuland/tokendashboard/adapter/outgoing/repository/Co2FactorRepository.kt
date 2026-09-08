package de.neuland.tokendashboard.adapter.outgoing.repository

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import java.time.LocalDate

interface Co2FactorRepository {
    @SqlQuery(
        """
        SELECT model, input_factor, output_factor, cache_read_factor, cache_write_factor
        FROM co2_factors
        WHERE model = :model
          AND valid_from <= :date
        ORDER BY valid_from DESC
        LIMIT 1
        """,
    )
    @RegisterKotlinMapper(Co2FactorRow::class)
    fun findLatestRateFor(
        @Bind("model") model: String,
        @Bind("date") date: LocalDate,
    ): Co2FactorRow?
}
