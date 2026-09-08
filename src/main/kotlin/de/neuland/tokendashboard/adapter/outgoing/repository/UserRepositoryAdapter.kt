package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.factories.TimeFactory
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.Provider
import de.neuland.tokendashboard.domain.model.UserCount
import de.neuland.tokendashboard.domain.model.UserId

class UserRepositoryAdapter(
    private val jdbi: UserRepository,
    private val timeFactory: TimeFactory,
) : UserRepositoryPort {
    override fun upsert(
        userId: UserId,
        provider: Provider,
        pluginVersion: PluginVersion,
    ) {
        val today = timeFactory.today().value
        jdbi.upsert(
            id = userId.value,
            firstDataSent = today,
            lastDataSent = today,
            provider = provider.name,
            pluginVersion = pluginVersion.value,
        )
    }

    override fun activeUsersLast4Weeks(provider: Provider): UserCount {
        val from = timeFactory.fourWeeksBack()
        return UserCount(jdbi.countActiveUsersFrom(from.value, provider.name))
    }

    override fun pluginInstallationsByDate(
        range: DayRange,
        granularity: Granularity,
        provider: Provider,
    ): Map<Day, UserCount> {
        val rows =
            when (granularity) {
                Granularity.DAY -> jdbi.countPluginInstallationsByDay(range.from.value, range.to.value, provider.name)
                Granularity.WEEK -> jdbi.countPluginInstallationsByWeek(range.from.value, range.to.value, provider.name)
            }
        return rows.associate { Day(it.date) to UserCount(it.count.toInt()) }
    }
}
