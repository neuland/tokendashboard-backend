package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.domain.model.AllProviderSeriesBucket
import de.neuland.tokendashboard.domain.model.AllProviderUsage
import de.neuland.tokendashboard.domain.model.AllProviderUsageSeries
import de.neuland.tokendashboard.domain.model.ProviderUsage
import kotlinx.serialization.Serializable

@Serializable
data class AllProviderUsageDto(
    val from: String,
    val to: String,
    val providerUsages: Map<String, ProviderUsageDto>,
)

@Serializable
data class ProviderUsageDto(
    val tokensInOut: Long,
    val totalEstimatedCo2: Double,
    val totalCostUsdCent: Long,
)

fun AllProviderUsage.toResponse() =
    AllProviderUsageDto(
        from = range.from.print(),
        to = range.to.print(),
        providerUsages = providerUsages.values.associate { it.provider.name to it.toResponse() },
    )

fun ProviderUsage.toResponse() =
    ProviderUsageDto(
        tokensInOut = tokensInOut.value,
        totalEstimatedCo2 = totalEstimatedCo2.value,
        totalCostUsdCent = totalCostUsdCent.value,
    )

@Serializable
data class AllProviderUsageSeriesResponseDto(
    val history: AllHistoryDto,
)

@Serializable
data class AllHistoryDto(
    val from: String,
    val to: String,
    val buckets: List<AllUsageBucketDto>,
)

@Serializable
data class AllUsageBucketDto(
    val date: String,
    val providers: Map<String, UsageBucketDto>,
)

fun AllProviderUsageSeries.toResponse() =
    AllProviderUsageSeriesResponseDto(
        history =
            AllHistoryDto(
                from = range.from.print(),
                to = range.to.print(),
                buckets = buckets.map { it.toResponse() },
            ),
    )

fun AllProviderSeriesBucket.toResponse(): AllUsageBucketDto {
    val providerDtos = providerBuckets.entries.associate { (provider, bucket) -> provider.name to bucket.toResponse(provider) }
    return AllUsageBucketDto(
        date = date.print(),
        providers = providerDtos,
    )
}
