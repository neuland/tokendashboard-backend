package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.domain.model.DatedModelUsages
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.OpenCodeUsageValuation

class OpenCodeUsageSource(
    private val usageRepository: OpenCodeUsageRepositoryPort,
    private val co2FactorRepository: Co2FactorRepositoryPort,
) : ProviderUsageSource {
    override fun dailyUsage(range: DayRange): List<DatedModelUsages> =
        OpenCodeUsageValuation(co2FactorRepository.co2FactorHistory()).valuate(usageRepository.openCodeDailyUsage(range))
}
