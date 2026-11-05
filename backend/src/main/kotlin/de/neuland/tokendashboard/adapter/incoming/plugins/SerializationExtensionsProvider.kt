package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.domain.model.ModelFamilyUsage
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.ModelUsages
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.ProviderUsageByModel
import de.neuland.tokendashboard.domain.model.ProviderUsageOverview
import de.neuland.tokendashboard.domain.model.UsageTotals
import de.neuland.tokendashboard.domain.model.totals
import kotlinx.serialization.Serializable

@Serializable
data class ProviderPluginUsageDto(
    val usage: ProviderUsageDetailDto,
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

fun ProviderUsageOverview.toResponse() =
    ProviderPluginUsageDto(
        usage = usage.toResponse(),
        activeUsersLast4Weeks = activeUsersLast4Weeks.value,
    )

fun ProviderUsageByModel.toResponse(): ProviderUsageDetailDto {
    val totals =
        when (modelUsages) {
            is ModelUsages.NoUsage -> null
            is ModelUsages.HasUsage -> modelUsages.totals().toDto()
        }
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
        tokens = modelUsages.totals().toDto(),
    )

fun ModelUsage.toResponse(provider: Provider) =
    ModelUsageDto(
        model = formatModelName(provider, model.value),
        UsageTotals(tokens, costUsdCent, estimatedCO2).toDto(),
    )
