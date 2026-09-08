package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.QueryAllUsagePort
import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.domain.model.AllProviderUsage
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.ProviderUsages

class AllUsageQueryService(
    private val claudeRepository: ClaudeUsageRepositoryPort,
    private val copilotRepository: CopilotUsageRepositoryPort,
    private val openCodeRepository: OpenCodeUsageRepositoryPort,
) : QueryAllUsagePort {
    override fun queryAllUsage(range: DayRange): AllProviderUsage =
        AllProviderUsage(
            range = range,
            providerUsages =
                ProviderUsages.of(
                    Provider.entries.map { provider ->
                        when (provider) {
                            CLAUDE -> claudeRepository.claudeProviderUsage(range)
                            COPILOT -> copilotRepository.copilotProviderUsage(range)
                            OPENCODE -> openCodeRepository.openCodeProviderUsage(range)
                        }
                    },
                ),
        )
}
