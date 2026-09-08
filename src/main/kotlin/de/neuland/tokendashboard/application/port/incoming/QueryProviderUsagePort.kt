package de.neuland.tokendashboard.application.port.incoming

import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.ProviderPluginUsage

interface QueryProviderUsagePort {
    fun queryUsage(
        provider: Provider,
        range: DayRange,
    ): ProviderPluginUsage
}
