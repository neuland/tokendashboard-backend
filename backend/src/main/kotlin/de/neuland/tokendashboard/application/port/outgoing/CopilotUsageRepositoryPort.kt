package de.neuland.tokendashboard.application.port.outgoing

import de.neuland.tokendashboard.domain.model.CopilotRawModelUsage
import de.neuland.tokendashboard.domain.model.CopilotUsageRecord
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange

interface CopilotUsageRepositoryPort {
    fun insertAll(records: List<CopilotUsageRecord>)

    fun copilotDailyUsage(range: DayRange): Map<Day, List<CopilotRawModelUsage>>
}
