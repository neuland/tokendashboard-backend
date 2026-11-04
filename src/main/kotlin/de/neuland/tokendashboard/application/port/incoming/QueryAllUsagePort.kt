package de.neuland.tokendashboard.application.port.incoming

import de.neuland.tokendashboard.domain.model.AllProviderTotals
import de.neuland.tokendashboard.domain.model.DayRange

interface QueryAllUsagePort {
    fun queryAllUsage(range: DayRange): AllProviderTotals
}
