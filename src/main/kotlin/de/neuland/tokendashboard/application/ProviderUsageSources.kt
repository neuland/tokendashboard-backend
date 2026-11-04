package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE

class ProviderUsageSources(
    private val claude: ProviderUsageSource,
    private val copilot: ProviderUsageSource,
    private val openCode: ProviderUsageSource,
) {
    fun of(provider: Provider): ProviderUsageSource =
        when (provider) {
            CLAUDE -> claude
            COPILOT -> copilot
            OPENCODE -> openCode
        }
}
