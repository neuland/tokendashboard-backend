package de.neuland.tokendashboard.adapter.outgoing.repository

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import java.time.LocalDate

interface ClaudePriceRepository {
    @SqlQuery(
        """
        SELECT DISTINCT ON (model) model, input_cent_per_million, output_cent_per_million,
               cache_write_5m_cent_per_million, cache_write_1h_cent_per_million,
               cache_read_cent_per_million
        FROM claude_prices
        WHERE valid_from <= :date
        ORDER BY model, valid_from DESC
        """,
    )
    @RegisterKotlinMapper(PriceRow::class)
    fun findAllLatestPrices(
        @Bind("date") date: LocalDate,
    ): List<PriceRow>

    @SqlQuery(
        """
        SELECT model, valid_from, input_cent_per_million, output_cent_per_million,
               cache_write_5m_cent_per_million, cache_write_1h_cent_per_million,
               cache_read_cent_per_million
        FROM claude_prices
        ORDER BY model, valid_from
        """,
    )
    @RegisterKotlinMapper(PriceVersionRow::class)
    fun findAllPriceVersions(): List<PriceVersionRow>
}
