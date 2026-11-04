package de.neuland.tokendashboard.adapter.outgoing.repository

import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery

interface Co2FactorRepository {
    @SqlQuery(
        """
        SELECT model, valid_from, input_factor, output_factor, cache_read_factor, cache_write_factor
        FROM co2_factors
        ORDER BY model, valid_from
        """,
    )
    @RegisterKotlinMapper(Co2FactorVersionRow::class)
    fun findAllFactorVersions(): List<Co2FactorVersionRow>
}
