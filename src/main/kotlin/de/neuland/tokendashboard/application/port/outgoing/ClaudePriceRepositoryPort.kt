package de.neuland.tokendashboard.application.port.outgoing

import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily

interface ClaudePriceRepositoryPort {
    fun priceFor(
        family: ModelFamily,
        date: Day,
    ): ClaudePrices?

    fun currentPricesPerFamily(date: Day): Map<ModelFamily, ClaudePrices>
}
