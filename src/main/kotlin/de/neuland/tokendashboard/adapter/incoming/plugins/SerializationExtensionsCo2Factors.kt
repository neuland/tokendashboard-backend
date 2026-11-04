package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.domain.model.Co2FactorVersions
import de.neuland.tokendashboard.domain.model.Co2Factors
import kotlinx.serialization.Serializable

@Serializable
data class Co2FactorsDto(
    val inputGramCo2PerMillion: Double,
    val outputGramCo2PerMillion: Double,
    val cacheReadGramCo2PerMillion: Double,
    val cacheWriteGramCo2PerMillion: Double,
)

@Serializable
data class Co2FactorVersionDto(
    val modelFamily: String,
    val validFrom: String,
    val factors: Co2FactorsDto,
)

fun Co2Factors.toResponse() =
    Co2FactorsDto(
        inputGramCo2PerMillion = inputFactor.gramCo2PerMillionToken,
        outputGramCo2PerMillion = outputFactor.gramCo2PerMillionToken,
        cacheReadGramCo2PerMillion = cacheReadFactor.gramCo2PerMillionToken,
        cacheWriteGramCo2PerMillion = cacheWriteFactor.gramCo2PerMillionToken,
    )

fun Co2FactorVersions.toResponse() =
    this.versions.map {
        Co2FactorVersionDto(
            modelFamily = it.family.value,
            validFrom = it.validFrom.print(),
            factors = it.factors.toResponse(),
        )
    }
