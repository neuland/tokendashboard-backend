package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.QueryCo2FactorHistoryPort
import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2FactorVersions

class Co2FactorHistoryQueryService(
    private val co2FactorRepository: Co2FactorRepositoryPort,
) : QueryCo2FactorHistoryPort {
    override fun queryCo2FactorHistory(): Co2FactorVersions = co2FactorRepository.co2FactorHistory()
}
