package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.QueryCurrentPricesPort
import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
import de.neuland.tokendashboard.domain.factories.TimeFactory
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.ModelFamily

class CurrentPricesQueryService(
    private val priceRepository: ClaudePriceRepositoryPort,
    private val timeFactory: TimeFactory,
) : QueryCurrentPricesPort {
    override fun queryCurrentPrices(): Map<ModelFamily, ClaudePrices> = priceRepository.currentPricesPerFamily(timeFactory.today())
}
