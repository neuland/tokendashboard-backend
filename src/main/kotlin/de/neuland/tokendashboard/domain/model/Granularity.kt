package de.neuland.tokendashboard.domain.model

import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

enum class Granularity {
    DAY,
    WEEK,
    ;

    fun bucketStart(day: Day): Day =
        when (this) {
            DAY -> day
            WEEK -> Day(day.value.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
        }
}
