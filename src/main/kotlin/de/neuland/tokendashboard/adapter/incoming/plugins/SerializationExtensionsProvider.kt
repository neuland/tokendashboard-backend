package de.neuland.tokendashboard.adapter.incoming.plugins

import arrow.core.NonEmptyList
import de.neuland.tokendashboard.domain.model.ModelFamilyUsage
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.ProviderPluginUsage
import de.neuland.tokendashboard.domain.model.ProviderUsageDetail
import de.neuland.tokendashboard.domain.model.UsageResult
import de.neuland.tokendashboard.domain.model.UsageTotals
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProviderPluginUsageDto(
    val usage: ProviderUsageDetailDto,
    @SerialName("activeUsersFor4Weeks")
    val activeUsersLast4Weeks: Int,
)

@Serializable
data class ProviderUsageDetailDto(
    val from: String,
    val to: String,
    val provider: ProviderWrapperDto,
)

@Serializable
data class ProviderWrapperDto(
    val providerName: String,
    val modelFamilies: List<ModelFamilyUsageDto>,
    val overallTotalTokens: DetailedUsageInfoDto?,
)

@Serializable
data class ModelFamilyUsageDto(
    val modelFamily: String,
    val models: List<ModelUsageDto>,
    val tokens: DetailedUsageInfoDto,
)

@Serializable
data class ModelUsageDto(
    val model: String,
    val tokens: DetailedUsageInfoDto,
)

@Serializable
data class DetailedUsageInfoDto(
    val inputTokens: Long,
    val outputTokens: Long,
    val cacheWriteTokens: Long,
    val cacheReadTokens: Long,
    val costUsdCent: Long,
    val estimatedCo2: Double,
)

private val versionDashRegex = Regex("(\\d+)-(\\d+)")

private fun formatModelName(
    provider: Provider,
    name: String,
): String =
    when (provider) {
        CLAUDE -> name.removePrefix("claude-").replace(versionDashRegex, "$1.$2")
        COPILOT -> name
        OPENCODE -> name
    }

fun ProviderPluginUsage.toResponse() =
    ProviderPluginUsageDto(
        usage = usage.toResponse(),
        activeUsersLast4Weeks = activeUsersLast4Weeks.value,
    )

fun ProviderUsageDetail.toResponse(): ProviderUsageDetailDto {
    val totals = sumUp(modelUsages)
    return ProviderUsageDetailDto(
        from = range.from.print(),
        to = range.to.print(),
        provider =
            ProviderWrapperDto(
                provider.name,
                modelUsagesByFamily().map { value -> value.toResponse(provider) },
                totals,
            ),
    )
}

fun sumUp(values: UsageResult): DetailedUsageInfoDto? =
    when (values) {
        is UsageResult.NoUsage -> null
        is UsageResult.HasUsage -> sumToEntry(values.entries)
    }

fun sumToEntry(entries: NonEmptyList<ModelUsage>): DetailedUsageInfoDto =
    entries.fold(UsageTotals.zero()) { acc, usage -> acc + usage }.toDto()

private fun UsageTotals.toDto(): DetailedUsageInfoDto =
    DetailedUsageInfoDto(
        inputTokens = tokens.inputTokens.value,
        outputTokens = tokens.outputTokens.value,
        cacheWriteTokens = tokens.cacheWriteTokens.value,
        cacheReadTokens = tokens.cacheReadTokens.value,
        costUsdCent = costUsdCent.value,
        estimatedCo2 = estimatedCO2.value,
    )

fun ModelFamilyUsage.toResponse(provider: Provider) =
    ModelFamilyUsageDto(
        modelFamily = modelFamily.value,
        models = modelUsages.map { it.toResponse(provider) },
        tokens = sumToEntry(modelUsages),
    )

fun ModelUsage.toResponse(provider: Provider) =
    ModelUsageDto(
        model = formatModelName(provider, model.value),
        UsageTotals(tokens, costUsdCent, estimatedCO2).toDto(),
    )
