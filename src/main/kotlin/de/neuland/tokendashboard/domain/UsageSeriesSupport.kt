package de.neuland.tokendashboard.domain

import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelUsagesAtDate
import de.neuland.tokendashboard.domain.model.UsageBucket
import de.neuland.tokendashboard.domain.model.UserCount

fun buildUsageBuckets(
    dayGroups: List<ModelUsagesAtDate>,
    pluginInstallationsByDate: Map<Day, UserCount>,
): List<UsageBucket> =
    dayGroups.map { group ->
        UsageBucket(
            date = group.date,
            modelUsages = group.modelUsages,
            pluginInstallations = pluginInstallationsByDate[group.date] ?: UserCount(0),
        )
    }
