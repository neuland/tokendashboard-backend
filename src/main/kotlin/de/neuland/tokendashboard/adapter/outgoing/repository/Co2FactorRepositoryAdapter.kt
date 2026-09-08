package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2Factors
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily

class Co2FactorRepositoryAdapter(
    private val jdbi: Co2FactorRepository,
) : Co2FactorRepositoryPort {
    override fun co2FactorsFor(
        family: ModelFamily,
        date: Day,
    ): Co2Factors? = jdbi.findLatestRateFor(family.value, date.value)?.toDomain()
}
