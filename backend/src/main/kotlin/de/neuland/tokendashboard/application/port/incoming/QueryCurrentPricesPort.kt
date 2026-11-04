package de.neuland.tokendashboard.application.port.incoming

import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.ModelFamily

interface QueryCurrentPricesPort {
    fun queryCurrentPrices(): Map<ModelFamily, ClaudePrices>
}
