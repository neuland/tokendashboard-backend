package de.neuland.tokendashboard.application.port.outgoing

import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.UserCount
import de.neuland.tokendashboard.domain.model.UserId

interface UserRepositoryPort {
    fun upsert(
        userId: UserId,
        provider: Provider,
        pluginVersion: PluginVersion,
    )

    fun activeUsersLast4Weeks(provider: Provider): UserCount

    fun pluginInstallationsByDate(
        range: DayRange,
        granularity: Granularity,
        provider: Provider,
    ): Map<Day, UserCount>
}
