package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2FactorVersions

class Co2FactorRepositoryAdapter(
    private val jdbi: Co2FactorRepository,
) : Co2FactorRepositoryPort {
    override fun co2FactorHistory(): Co2FactorVersions = Co2FactorVersions(jdbi.findAllFactorVersions().map { it.toDomain() })
}
