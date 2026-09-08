package de.neuland.tokendashboard.domain.factories

import de.neuland.tokendashboard.domain.model.Day
import java.time.LocalDate
import java.time.ZoneOffset

class TimeFactory {
    fun today(): Day = Day(LocalDate.now(ZoneOffset.UTC))

    fun fourWeeksBack(): Day = Day(LocalDate.now(ZoneOffset.UTC).minusWeeks(4))
}
