package de.neuland.tokendashboard.application

import arrow.core.nonEmptyListOf
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.DatedModelUsages
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.ModelUsage
import de.neuland.tokendashboard.domain.model.ModelUsages
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.ProviderUsageByModel
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UserCount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class ProviderUsageQueryServiceTest :
    FunSpec({
        val from = Day(LocalDate.of(2026, 6, 1))
        val to = Day(LocalDate.of(2026, 6, 15))
        val range = DayRange.of(from, to)

        fun modelUsage(
            model: String,
            inputTokens: Long,
            costUsdCent: Long,
        ) = ModelUsage(
            model = ModelName.from(model),
            tokens = TokenCounts(TokenCount(inputTokens), TokenCount.ZERO, TokenCount.ZERO, TokenCount.ZERO),
            costUsdCent = DollarCent(costUsdCent),
            estimatedCO2 = Co2Gram(1.0),
        )

        val claudeSource = mockk<ProviderUsageSource>()
        val copilotSource = mockk<ProviderUsageSource>()
        val openCodeSource = mockk<ProviderUsageSource>()
        val userRepository = mockk<UserRepositoryPort>()

        every { claudeSource.dailyUsage(any()) } returns emptyList()
        every { copilotSource.dailyUsage(any()) } returns emptyList()
        every { openCodeSource.dailyUsage(any()) } returns emptyList()
        every { userRepository.activeUsersLast4Weeks(any()) } returns UserCount(42)

        val service = ProviderUsageQueryService(ProviderUsageSources(claudeSource, copilotSource, openCodeSource), userRepository)

        test("query usage sums the day groups per model over the range") {
            every { claudeSource.dailyUsage(any()) } returns
                listOf(
                    DatedModelUsages(to, ModelUsages.HasUsage(nonEmptyListOf(modelUsage("claude-sonnet-4-5", 10, 1)))),
                    DatedModelUsages(
                        from,
                        ModelUsages.HasUsage(
                            nonEmptyListOf(modelUsage("claude-sonnet-4-5", 20, 2), modelUsage("claude-opus-4-5", 5, 3)),
                        ),
                    ),
                )

            val result = service.queryUsage(CLAUDE, range)

            val entries = (result.usage.modelUsages as ModelUsages.HasUsage).entries.all.associateBy { it.model }
            entries.getValue(ModelName.from("claude-sonnet-4-5")).tokens.inputTokens shouldBe TokenCount(30)
            entries.getValue(ModelName.from("claude-sonnet-4-5")).costUsdCent shouldBe DollarCent(3)
            entries.getValue(ModelName.from("claude-opus-4-5")).tokens.inputTokens shouldBe TokenCount(5)
            result.activeUsersLast4Weeks shouldBe UserCount(42)
        }

        test("query usage returns NoUsage when the range has no usage") {
            every { claudeSource.dailyUsage(any()) } returns emptyList()

            service.queryUsage(CLAUDE, range).usage shouldBe ProviderUsageByModel(CLAUDE, range, ModelUsages.NoUsage)
        }

        test("query usage reads from the source of the requested provider") {
            every { copilotSource.dailyUsage(range) } returns
                listOf(DatedModelUsages(from, ModelUsages.HasUsage(nonEmptyListOf(modelUsage("gpt-5.4-mini", 7, 1)))))

            service.queryUsage(COPILOT, range).usage shouldBe
                ProviderUsageByModel(COPILOT, range, ModelUsages.HasUsage(nonEmptyListOf(modelUsage("gpt-5.4-mini", 7, 1))))
            service.queryUsage(OPENCODE, range).usage shouldBe ProviderUsageByModel(OPENCODE, range, ModelUsages.NoUsage)
        }
    })
