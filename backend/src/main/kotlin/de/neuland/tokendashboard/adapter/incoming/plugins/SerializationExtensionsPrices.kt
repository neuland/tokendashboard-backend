package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.ModelFamily
import kotlinx.serialization.Serializable

@Serializable
data class ClaudePricesDto(
    val inputCentPerMillion: Long,
    val outputCentPerMillion: Long,
    val cacheWrite5mCentPerMillion: Long,
    val cacheWrite1hCentPerMillion: Long,
    val cacheWriteCentPerMillion: Long,
    val cacheReadCentPerMillion: Long,
)

fun ClaudePrices.toResponse() =
    ClaudePricesDto(
        inputCentPerMillion = inputCentPerMillion.value,
        outputCentPerMillion = outputCentPerMillion.value,
        cacheWrite5mCentPerMillion = cacheWrite5mCentPerMillion.value,
        cacheWrite1hCentPerMillion = cacheWrite1hCentPerMillion.value,
        cacheWriteCentPerMillion = cacheWrite1hCentPerMillion.value,
        cacheReadCentPerMillion = cacheReadCentPerMillion.value,
    )

fun Map<ModelFamily, ClaudePrices>.toResponse(): Map<String, ClaudePricesDto> = mapKeys { it.key.value }.mapValues { it.value.toResponse() }
