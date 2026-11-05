package de.neuland.tokendashboard.application.port.outgoing

import de.neuland.tokendashboard.domain.model.Co2FactorVersions

interface Co2FactorRepositoryPort {
    fun co2FactorHistory(): Co2FactorVersions
}
