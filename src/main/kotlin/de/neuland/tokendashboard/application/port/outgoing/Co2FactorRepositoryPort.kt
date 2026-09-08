package de.neuland.tokendashboard.application.port.outgoing

import de.neuland.tokendashboard.domain.model.Co2Factors
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily

interface Co2FactorRepositoryPort {
    fun co2FactorsFor(
        family: ModelFamily,
        date: Day,
    ): Co2Factors?
}
