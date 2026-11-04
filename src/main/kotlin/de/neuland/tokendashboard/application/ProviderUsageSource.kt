package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.domain.model.DatedModelUsages
import de.neuland.tokendashboard.domain.model.DayRange

interface ProviderUsageSource {
    fun dailyUsage(range: DayRange): List<DatedModelUsages>
}
