package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.Co2Factor
import de.neuland.tokendashboard.domain.model.Co2Factors
import org.jdbi.v3.core.mapper.reflect.ColumnName

data class Co2FactorRow(
    val model: String,
    @param:ColumnName("input_factor") val inputFactor: Double,
    @param:ColumnName("output_factor") val outputFactor: Double,
    @param:ColumnName("cache_read_factor") val cacheReadFactor: Double,
    @param:ColumnName("cache_write_factor") val cacheWriteFactor: Double,
) {
    fun toDomain() =
        Co2Factors(
            inputFactor = Co2Factor(inputFactor),
            outputFactor = Co2Factor(outputFactor),
            cacheReadFactor = Co2Factor(cacheReadFactor),
            cacheWriteFactor = Co2Factor(cacheWriteFactor),
        )
}
