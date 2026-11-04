package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.domain.model.ClaudeUsageValuation
import de.neuland.tokendashboard.domain.model.DatedModelUsages
import de.neuland.tokendashboard.domain.model.DayRange

class ClaudeUsageSource(
    private val usageRepository: ClaudeUsageRepositoryPort,
    private val priceRepository: ClaudePriceRepositoryPort,
    private val co2FactorRepository: Co2FactorRepositoryPort,
) : ProviderUsageSource {
    override fun dailyUsage(range: DayRange): List<DatedModelUsages> {
        val valuation = ClaudeUsageValuation(priceRepository.priceHistory(), co2FactorRepository.co2FactorHistory())
        return valuation.valuate(usageRepository.claudeDailyUsage(range))
    }
}
