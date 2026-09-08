package de.neuland.tokendashboard.domain.model

import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ProviderUsagesTest :
    FunSpec({
        fun usage(provider: Provider) = ProviderUsage(provider, TokenCount(1), Co2Gram(1.0), DollarCent(1))

        test("keeps one usage per provider") {
            val values = listOf(usage(CLAUDE), usage(COPILOT))

            val providerUsages = ProviderUsages.of(values)

            providerUsages.values shouldBe values
        }

        test("rejects two usages for the same provider") {
            val exception = shouldThrow<IllegalArgumentException> { ProviderUsages.of(listOf(usage(CLAUDE), usage(CLAUDE))) }

            exception.message shouldContain "CLAUDE"
        }
    })
