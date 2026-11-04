package de.neuland.tokendashboard.testsupport

import de.neuland.tokendashboard.domain.factories.TimeFactory
import de.neuland.tokendashboard.domain.model.Day
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

fun fixedTimeFactory(date: LocalDate): TimeFactory {
    val timeFactory = mockk<TimeFactory>()
    every { timeFactory.today() } returns Day(date)
    every { timeFactory.fourWeeksBack() } returns Day(date.minusWeeks(4))
    return timeFactory
}
