package de.neuland.tokendashboard.application.port.incoming

import de.neuland.tokendashboard.domain.model.ClaudePriceVersions

interface QueryPriceHistoryPort {
    fun queryPriceHistory(): ClaudePriceVersions
}
