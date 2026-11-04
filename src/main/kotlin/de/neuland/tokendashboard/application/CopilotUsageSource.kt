package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.domain.model.CopilotUsageValuation
import de.neuland.tokendashboard.domain.model.DatedModelUsages
import de.neuland.tokendashboard.domain.model.DayRange

class CopilotUsageSource(
    private val usageRepository: CopilotUsageRepositoryPort,
    private val co2FactorRepository: Co2FactorRepositoryPort,
) : ProviderUsageSource {
    override fun dailyUsage(range: DayRange): List<DatedModelUsages> =
        CopilotUsageValuation(co2FactorRepository.co2FactorHistory()).valuate(usageRepository.copilotDailyUsage(range))
}
