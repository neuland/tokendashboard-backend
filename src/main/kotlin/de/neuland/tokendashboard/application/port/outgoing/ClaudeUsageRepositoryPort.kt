package de.neuland.tokendashboard.application.port.outgoing

import de.neuland.tokendashboard.domain.model.ClaudeRawModelUsage
import de.neuland.tokendashboard.domain.model.ClaudeUsageRecord
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange

interface ClaudeUsageRepositoryPort {
    fun insertAll(records: List<ClaudeUsageRecord>)

    fun claudeDailyUsage(range: DayRange): Map<Day, List<ClaudeRawModelUsage>>
}
