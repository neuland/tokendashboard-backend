package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.QueryProviderUsagePort
import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.ProviderPluginUsage

class ProviderUsageQueryService(
    private val claudeUsageRepository: ClaudeUsageRepositoryPort,
    private val copilotUsageRepository: CopilotUsageRepositoryPort,
    private val openCodeUsageRepository: OpenCodeUsageRepositoryPort,
    private val userRepository: UserRepositoryPort,
) : QueryProviderUsagePort {
    override fun queryUsage(
        provider: Provider,
        range: DayRange,
    ): ProviderPluginUsage {
        val usage =
            when (provider) {
                CLAUDE -> claudeUsageRepository.claudeUsageByDateRange(range)
                COPILOT -> copilotUsageRepository.copilotUsageByDateRange(range)
                OPENCODE -> openCodeUsageRepository.openCodeUsageByDateRange(range)
            }
        return ProviderPluginUsage(
            usage = usage,
            activeUsersLast4Weeks = userRepository.activeUsersLast4Weeks(provider),
        )
    }
}
