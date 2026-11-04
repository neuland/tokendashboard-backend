package de.neuland.tokendashboard.domain.model

data class ClaudePriceVersion(
    val family: ModelFamily,
    val validFrom: Day,
    val prices: ClaudePrices,
)

data class ClaudePriceVersions(
    val versions: List<ClaudePriceVersion>,
) {
    private val byFamily: Map<ModelFamily, List<ClaudePriceVersion>> =
        versions
            .groupBy { it.family }
            .mapValues { (_, v) -> v.sortedBy { it.validFrom } }

    fun pricesFor(
        model: ModelName,
        key: Day,
    ): ClaudePrices =
        byFamily[model.family()]
            ?.let { familyVersions -> familyVersions.lastOrNull { it.validFrom <= key } ?: familyVersions.first() }
            ?.prices
            ?: ClaudePrices.ZERO
}
