package de.neuland.tokendashboard.application.port.outgoing

import de.neuland.tokendashboard.domain.model.ClaudePriceVersions
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily

interface ClaudePriceRepositoryPort {
    fun currentPricesPerFamily(date: Day): Map<ModelFamily, ClaudePrices>

    fun priceHistory(): ClaudePriceVersions
}
