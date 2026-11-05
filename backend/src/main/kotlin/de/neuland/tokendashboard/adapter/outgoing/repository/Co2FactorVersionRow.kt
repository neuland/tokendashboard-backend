package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.model.Co2Factor
import de.neuland.tokendashboard.domain.model.Co2FactorVersion
import de.neuland.tokendashboard.domain.model.Co2Factors
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily
import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.LocalDate

data class Co2FactorVersionRow(
    val model: String,
    @param:ColumnName("valid_from") val validFrom: LocalDate,
    @param:ColumnName("input_factor") val inputFactor: Double,
    @param:ColumnName("output_factor") val outputFactor: Double,
    @param:ColumnName("cache_read_factor") val cacheReadFactor: Double,
    @param:ColumnName("cache_write_factor") val cacheWriteFactor: Double,
) {
    fun toDomain() =
        Co2FactorVersion(
            family = ModelFamily(model),
            validFrom = Day(validFrom),
            factors =
                Co2Factors(
                    inputFactor = Co2Factor(inputFactor),
                    outputFactor = Co2Factor(outputFactor),
                    cacheReadFactor = Co2Factor(cacheReadFactor),
                    cacheWriteFactor = Co2Factor(cacheWriteFactor),
                ),
        )
}
