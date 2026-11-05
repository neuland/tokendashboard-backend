package de.neuland.tokendashboard.domain.model

data class AllProviderTotals(
    val range: DayRange,
    val totals: ProviderTotalsByProvider,
)

class ProviderTotalsByProvider private constructor(
    val byProvider: Map<Provider, UsageTotals>,
) {
    companion object {
        fun forAllProviders(totalsOf: (Provider) -> UsageTotals) = ProviderTotalsByProvider(Provider.entries.associateWith(totalsOf))
    }
}
