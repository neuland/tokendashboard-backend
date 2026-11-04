package de.neuland.tokendashboard.domain.model

fun <T> Map<Day, List<T>>.valuateDaily(valuate: (Day, T) -> ModelUsage): List<DatedModelUsages> =
    map { (day, usages) -> DatedModelUsages(day, usages.map { valuate(day, it) }.toModelUsages()) }
