package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.QueryPriceHistoryPort
import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
import de.neuland.tokendashboard.domain.model.ClaudePriceVersions

class PriceHistoryQueryService(
    private val priceRepository: ClaudePriceRepositoryPort,
) : QueryPriceHistoryPort {
    override fun queryPriceHistory(): ClaudePriceVersions = priceRepository.priceHistory()
}
