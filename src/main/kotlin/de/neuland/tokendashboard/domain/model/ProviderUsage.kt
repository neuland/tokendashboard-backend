package de.neuland.tokendashboard.domain.model

data class AllProviderUsage(
    val range: DayRange,
    val providerUsages: ProviderUsages,
)

data class ProviderUsage(
    val provider: Provider,
    val tokensInOut: TokenCount,
    val totalEstimatedCo2: Co2Gram,
    val totalCostUsdCent: DollarCent,
)

class ProviderUsages private constructor(
    val values: List<ProviderUsage>,
) {
    companion object {
        fun of(values: List<ProviderUsage>): ProviderUsages {
            val duplicates = values.groupBy { it.provider }.filterValues { it.size > 1 }.keys
            require(duplicates.isEmpty()) { "duplicate provider(s) in ProviderUsages: $duplicates" }
            return ProviderUsages(values)
        }
    }
}
