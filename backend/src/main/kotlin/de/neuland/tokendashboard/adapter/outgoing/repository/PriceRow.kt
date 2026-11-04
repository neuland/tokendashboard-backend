package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.CentPerMillionToken
import de.neuland.tokendashboard.domain.model.ClaudePrices
import org.jdbi.v3.core.mapper.reflect.ColumnName

data class PriceRow(
    val model: String,
    @param:ColumnName("input_cent_per_million") val inputCentPerMillion: Long,
    @param:ColumnName("output_cent_per_million") val outputCentPerMillion: Long,
    @param:ColumnName("cache_write_5m_cent_per_million") val cacheWrite5mCentPerMillion: Long,
    @param:ColumnName("cache_write_1h_cent_per_million") val cacheWrite1hCentPerMillion: Long,
    @param:ColumnName("cache_read_cent_per_million") val cacheReadCentPerMillion: Long,
) {
    fun toDomain() =
        ClaudePrices(
            inputCentPerMillion = CentPerMillionToken(inputCentPerMillion),
            outputCentPerMillion = CentPerMillionToken(outputCentPerMillion),
            cacheWrite5mCentPerMillion = CentPerMillionToken(cacheWrite5mCentPerMillion),
            cacheWrite1hCentPerMillion = CentPerMillionToken(cacheWrite1hCentPerMillion),
            cacheReadCentPerMillion = CentPerMillionToken(cacheReadCentPerMillion),
        )
}
