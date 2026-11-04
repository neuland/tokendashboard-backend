package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.mockk

class ProviderUsageSourcesTest :
    FunSpec({
        test("of returns the source registered for each provider") {
            val claude = mockk<ProviderUsageSource>()
            val copilot = mockk<ProviderUsageSource>()
            val openCode = mockk<ProviderUsageSource>()
            val sources = ProviderUsageSources(claude, copilot, openCode)

            sources.of(CLAUDE) shouldBeSameInstanceAs claude
            sources.of(COPILOT) shouldBeSameInstanceAs copilot
            sources.of(OPENCODE) shouldBeSameInstanceAs openCode
        }
    })
