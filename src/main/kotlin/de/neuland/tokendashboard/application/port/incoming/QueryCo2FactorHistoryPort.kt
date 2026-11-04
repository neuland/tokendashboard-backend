package de.neuland.tokendashboard.application.port.incoming

import de.neuland.tokendashboard.domain.model.Co2FactorVersions

interface QueryCo2FactorHistoryPort {
    fun queryCo2FactorHistory(): Co2FactorVersions
}
