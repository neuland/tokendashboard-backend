package de.neuland.tokendashboard.domain.model

data class Co2FactorVersion(
    val family: ModelFamily,
    val validFrom: Day,
    val factors: Co2Factors,
)

data class Co2FactorVersions(
    val versions: List<Co2FactorVersion>,
) {
    private val byFamily: Map<ModelFamily, List<Co2FactorVersion>> =
        versions
            .groupBy { it.family }
            .mapValues { (_, v) -> v.sortedBy { it.validFrom } }

    fun co2FactorsFor(
        model: ModelName,
        key: Day,
    ): Co2Factors =
        byFamily[model.family()]
            ?.let { familyVersions -> familyVersions.lastOrNull { it.validFrom <= key } ?: familyVersions.first() }
            ?.factors
            ?: Co2Factors.ZERO
}
