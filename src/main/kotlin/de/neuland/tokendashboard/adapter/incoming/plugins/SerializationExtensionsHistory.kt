package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.UsageBucket
import de.neuland.tokendashboard.domain.model.UsageSeries
import kotlinx.serialization.Serializable

@Serializable
data class UsageSeriesResponseDto(
    val history: HistoryDto,
)

@Serializable
data class HistoryDto(
    val from: String,
    val to: String,
    val provider: SeriesProviderDto,
)

@Serializable
data class SeriesProviderDto(
    val providerName: String,
    val buckets: List<UsageBucketDto>,
)

@Serializable
data class UsageBucketDto(
    val date: String,
    val modelFamilies: List<ModelFamilyUsageDto>,
    val overallTotalTokens: DetailedUsageInfoDto,
    val pluginInstallations: Int,
)

fun UsageSeries.toResponse(provider: Provider): UsageSeriesResponseDto =
    UsageSeriesResponseDto(
        history =
            HistoryDto(
                from = range.from.print(),
                to = range.to.print(),
                provider =
                    SeriesProviderDto(
                        providerName = provider.name,
                        buckets = buckets.map { it.toResponse(provider) },
                    ),
            ),
    )

fun UsageBucket.toResponse(provider: Provider): UsageBucketDto =
    UsageBucketDto(
        date = date.print(),
        modelFamilies = modelFamilies.map { it.toResponse(provider) },
        overallTotalTokens =
            DetailedUsageInfoDto(
                inputTokens = overallTotal.tokens.inputTokens.value,
                outputTokens = overallTotal.tokens.outputTokens.value,
                cacheWriteTokens = overallTotal.tokens.cacheWriteTokens.value,
                cacheReadTokens = overallTotal.tokens.cacheReadTokens.value,
                costUsdCent = overallTotal.costUsdCent.value,
                estimatedCo2 = overallTotal.estimatedCO2.value,
            ),
        pluginInstallations = pluginInstallations.value,
    )
