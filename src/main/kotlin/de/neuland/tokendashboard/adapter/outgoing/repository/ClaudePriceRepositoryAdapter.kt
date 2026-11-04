package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
import de.neuland.tokendashboard.domain.model.ClaudePriceVersions
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily

class ClaudePriceRepositoryAdapter(
    private val jdbi: ClaudePriceRepository,
) : ClaudePriceRepositoryPort {
    override fun currentPricesPerFamily(date: Day): Map<ModelFamily, ClaudePrices> =
        jdbi.findAllLatestPrices(date.value).associate { ModelFamily(it.model) to it.toDomain() }

    override fun priceHistory(): ClaudePriceVersions = ClaudePriceVersions(jdbi.findAllPriceVersions().map { it.toDomain() })
}
