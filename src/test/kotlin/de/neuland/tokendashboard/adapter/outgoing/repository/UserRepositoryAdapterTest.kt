package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.domain.factories.TimeFactory
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.UserCount
import de.neuland.tokendashboard.domain.model.UserId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class UserRepositoryAdapterTest :
    FunSpec({

        val jdbi = mockk<UserRepository>()

        test("upsert delegates unwrapped values to jdbi") {
            val timeFactory = mockk<TimeFactory>()
            val adapter = UserRepositoryAdapter(jdbi, timeFactory)
            val today = Day(LocalDate.of(2026, 6, 15))
            every { timeFactory.today() } returns today
            every { jdbi.upsert(any(), any(), any(), any(), any()) } just Runs

            adapter.upsert(UserId("user-1"), CLAUDE, PluginVersion("1.2.3"))

            verify { jdbi.upsert("user-1", LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 15), "CLAUDE", "1.2.3") }
        }

        test("activeUsersLast4Weeks queries from timeFactory.fourWeeksBack, not today") {
            val timeFactory = mockk<TimeFactory>()
            val adapter = UserRepositoryAdapter(jdbi, timeFactory)
            every { timeFactory.fourWeeksBack() } returns Day(LocalDate.of(2026, 5, 18))
            every { jdbi.countActiveUsersFrom(LocalDate.of(2026, 5, 18), "CLAUDE") } returns 3

            val result = adapter.activeUsersLast4Weeks(CLAUDE)

            result shouldBe UserCount(3)
            verify { jdbi.countActiveUsersFrom(LocalDate.of(2026, 5, 18), "CLAUDE") }
            verify(exactly = 0) { timeFactory.today() }
        }
    })
