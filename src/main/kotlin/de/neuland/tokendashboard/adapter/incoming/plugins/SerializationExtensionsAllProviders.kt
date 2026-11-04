package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.domain.model.AllProviderSeriesBucket
import de.neuland.tokendashboard.domain.model.AllProviderTotals
import de.neuland.tokendashboard.domain.model.AllProviderUsageSeries
import de.neuland.tokendashboard.domain.model.UsageTotals
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

fun AllProviderTotals.toResponse() =
    AllProviderUsageDto(
        from = range.from.print(),
        to = range.to.print(),
        providerUsages = totals.byProvider.entries.associate { (provider, providerTotals) -> provider.name to providerTotals.toResponse() },
    )

fun UsageTotals.toResponse() =
    ProviderUsageDto(
        tokensInOut = tokensInOut().value,
        totalEstimatedCo2 = estimatedCO2.value,
        totalCostUsdCent = costUsdCent.value,
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
