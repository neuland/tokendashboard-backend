package de.neuland.tokendashboard.domain

import de.neuland.tokendashboard.domain.model.DatedModelUsages
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.ModelUsages
import de.neuland.tokendashboard.domain.model.UsageBucket
import de.neuland.tokendashboard.domain.model.UserCount
import de.neuland.tokendashboard.domain.model.plus

fun buildUsageBuckets(
    dayGroups: List<DatedModelUsages>,
    pluginInstallationsByDate: Map<Day, UserCount>,
): List<UsageBucket> =
    dayGroups.map { group ->
        UsageBucket(
            date = group.date,
            modelUsages = group.modelUsages,
            pluginInstallations = pluginInstallationsByDate[group.date] ?: UserCount(0),
        )
    }

fun List<DatedModelUsages>.rollUp(granularity: Granularity): List<DatedModelUsages> =
    groupBy { granularity.bucketStart(it.date) }
        .map { (bucketStart, days) ->
            DatedModelUsages(bucketStart, days.map { it.modelUsages }.reduce { acc, usage -> acc + usage })
        }.sortedByDescending { it.date }

fun List<DatedModelUsages>.sumByModel(): ModelUsages =
    map { it.modelUsages }.fold(ModelUsages.NoUsage as ModelUsages) { acc, usage -> acc + usage }
