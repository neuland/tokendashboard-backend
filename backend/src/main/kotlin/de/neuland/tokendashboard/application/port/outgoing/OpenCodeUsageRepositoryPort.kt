package de.neuland.tokendashboard.application.port.outgoing

import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.OpenCodeRawModelUsage
import de.neuland.tokendashboard.domain.model.OpenCodeUsageRecord

interface OpenCodeUsageRepositoryPort {
    fun insertAll(records: List<OpenCodeUsageRecord>)

    fun openCodeDailyUsage(range: DayRange): Map<Day, List<OpenCodeRawModelUsage>>
}
