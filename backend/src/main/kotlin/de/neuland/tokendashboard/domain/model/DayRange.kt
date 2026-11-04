package de.neuland.tokendashboard.domain.model

import java.time.temporal.ChronoUnit

data class DayRange private constructor(
    val from: Day,
    val to: Day,
) {
    companion object {
        const val MAX_RANGE_DAYS = 365 * 5 + 2L // 5 years + leap days

        fun of(
            from: Day,
            to: Day,
        ): DayRange {
            require(from.value <= to.value) {
                "range must be ordered: from=${from.print()} is after to=${to.print()}"
            }
            val spannedDays = ChronoUnit.DAYS.between(from.value, to.value)
            require(spannedDays <= MAX_RANGE_DAYS) {
                "range must span at most $MAX_RANGE_DAYS days, but spans $spannedDays"
            }
            return DayRange(from, to)
        }
    }
}
