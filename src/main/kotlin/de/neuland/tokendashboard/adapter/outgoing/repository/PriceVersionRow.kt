package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.CentPerMillionToken
import de.neuland.tokendashboard.domain.model.ClaudePriceVersion
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily
import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.LocalDate

data class PriceVersionRow(
    val model: String,
    @param:ColumnName("valid_from") val validFrom: LocalDate,
    @param:ColumnName("input_cent_per_million") val inputCentPerMillion: Long,
    @param:ColumnName("output_cent_per_million") val outputCentPerMillion: Long,
    @param:ColumnName("cache_write_5m_cent_per_million") val cacheWrite5mCentPerMillion: Long,
    @param:ColumnName("cache_write_1h_cent_per_million") val cacheWrite1hCentPerMillion: Long,
    @param:ColumnName("cache_read_cent_per_million") val cacheReadCentPerMillion: Long,
) {
    fun toDomain() =
        ClaudePriceVersion(
            family = ModelFamily(model),
            validFrom = Day(validFrom),
            prices =
                ClaudePrices(
                    inputCentPerMillion = CentPerMillionToken(inputCentPerMillion),
                    outputCentPerMillion = CentPerMillionToken(outputCentPerMillion),
                    cacheWrite5mCentPerMillion = CentPerMillionToken(cacheWrite5mCentPerMillion),
                    cacheWrite1hCentPerMillion = CentPerMillionToken(cacheWrite1hCentPerMillion),
                    cacheReadCentPerMillion = CentPerMillionToken(cacheReadCentPerMillion),
                ),
        )
}
