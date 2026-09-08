package de.neuland.tokendashboard.application

import arrow.core.nonEmptyListOf
import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.ClaudeUsageRecord
import de.neuland.tokendashboard.domain.model.Co2Factor
import de.neuland.tokendashboard.domain.model.Co2Factors
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.IncomingCacheWriteTokens
import de.neuland.tokendashboard.domain.model.IncomingTokenCounts
import de.neuland.tokendashboard.domain.model.ModelFamily
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptId
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.UserCount
import de.neuland.tokendashboard.domain.model.UserId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant

class ClaudeUsageIngestServiceTest :
    FunSpec({

        val fixedTimestamp = PromptTimestamp(Instant.parse("2026-06-12T10:00:00Z"))

        val noFactor = mockk<Co2FactorRepositoryPort>()
        val noPrice = mockk<ClaudePriceRepositoryPort>()
        val noUserRepo = mockk<UserRepositoryPort>()
        val repository = mockk<ClaudeUsageRepositoryPort>()

        fun service(
            co2FactorRepository: Co2FactorRepositoryPort = noFactor,
            priceRepository: ClaudePriceRepositoryPort = noPrice,
        ) = ClaudeUsageIngestService(repository, noUserRepo, co2FactorRepository, priceRepository)

        beforeTest {
            clearAllMocks()
            every { noFactor.co2FactorsFor(any(), any()) } returns null
            every { noPrice.priceFor(any(), any()) } returns null
            every { noUserRepo.upsert(any(), any(), any()) } just Runs
            every { noUserRepo.activeUsersLast4Weeks(any()) } returns UserCount(0)
            every { repository.insertAll(any()) } just Runs
        }

        test("ingest saves one record per model") {
            service().ingest(
                IngestClaudeUsageRecordCommand(
                    userId = UserId("user-1"),
                    prompts = nonEmptyListOf(promptUsage(ModelName.from("opus")), promptUsage(ModelName.from("sonnet"))),
                    pluginVersion = PluginVersion("1.0.0"),
                ),
            )

            verify(exactly = 1) { repository.insertAll(match { it.size == 2 }) }
        }

        test("ingest uses command timestamp") {
            val slot = slot<List<ClaudeUsageRecord>>()
            every { repository.insertAll(capture(slot)) } just Runs

            service().ingest(
                IngestClaudeUsageRecordCommand(
                    userId = UserId("user-1"),
                    prompts = nonEmptyListOf(promptUsage(ModelName.from("opus"), fixedTimestamp)),
                    pluginVersion = PluginVersion("1.0.0"),
                ),
            )

            slot.captured.single().timestamp shouldBe fixedTimestamp
        }

        test("ingest sets co2 gram to zero when no rate found") {
            val slot = slot<List<ClaudeUsageRecord>>()
            every { repository.insertAll(capture(slot)) } just Runs

            service().ingest(
                IngestClaudeUsageRecordCommand(
                    userId = UserId("user-1"),
                    prompts = nonEmptyListOf(promptUsage(ModelName.from("unknown"))),
                    pluginVersion = PluginVersion("1.0.0"),
                ),
            )

            slot.captured.single().co2Gram shouldBe Co2Gram(0.0)
        }

        test("ingest passes co2 gram from rate to repository") {
            val slot = slot<List<ClaudeUsageRecord>>()
            every { repository.insertAll(capture(slot)) } just Runs

            val sonnetFactors =
                Co2Factors(
                    inputFactor = Co2Factor(42.0),
                    outputFactor = Co2Factor(840.0),
                    cacheReadFactor = Co2Factor(0.42),
                    cacheWriteFactor = Co2Factor(52.0),
                )
            val stubRate = mockk<Co2FactorRepositoryPort>()
            every { stubRate.co2FactorsFor(any(), any()) } returns sonnetFactors

            val usage = promptUsage(ModelName.from("sonnet"))
            service(co2FactorRepository = stubRate).ingest(
                IngestClaudeUsageRecordCommand(
                    userId = UserId("user-1"),
                    prompts = nonEmptyListOf(usage),
                    pluginVersion = PluginVersion("1.0.0"),
                ),
            )

            slot.captured.single().co2Gram shouldBe sonnetFactors.calculateCo2Gram(usage.tokenCounts)
        }

        test("ingest looks up factor and price only once per model and day") {
            val stubRate = mockk<Co2FactorRepositoryPort>()
            val stubPrice = mockk<ClaudePriceRepositoryPort>()
            every { stubRate.co2FactorsFor(any(), any()) } returns null
            every { stubPrice.priceFor(any(), any()) } returns null

            val prompts =
                nonEmptyListOf(
                    promptUsage(ModelName.from("sonnet"), fixedTimestamp),
                    promptUsage(ModelName.from("sonnet"), fixedTimestamp),
                    promptUsage(ModelName.from("sonnet"), fixedTimestamp),
                )
            service(co2FactorRepository = stubRate, priceRepository = stubPrice).ingest(
                IngestClaudeUsageRecordCommand(userId = UserId("user-1"), prompts = prompts, pluginVersion = PluginVersion("1.0.0")),
            )

            verify(exactly = 1) { stubRate.co2FactorsFor(ModelFamily("sonnet"), any()) }
            verify(exactly = 1) { stubPrice.priceFor(ModelFamily("sonnet"), any()) }
            verify(exactly = 1) { repository.insertAll(match { it.size == 3 }) }
        }

        test("pluginVersion from the command is set on every record") {
            val slot = slot<List<ClaudeUsageRecord>>()
            every { repository.insertAll(capture(slot)) } just Runs

            service().ingest(
                IngestClaudeUsageRecordCommand(
                    userId = UserId("user-1"),
                    prompts = nonEmptyListOf(promptUsage(ModelName.from("opus")), promptUsage(ModelName.from("sonnet"))),
                    pluginVersion = PluginVersion("1.2.3"),
                ),
            )

            slot.captured.map { it.pluginVersion } shouldBe listOf(PluginVersion("1.2.3"), PluginVersion("1.2.3"))
        }
    })

private fun promptUsage(
    model: ModelName,
    timestamp: PromptTimestamp = PromptTimestamp(Instant.parse("2026-06-12T10:00:00Z")),
) = IngestClaudeUsageRecordCommand.Prompt(
    model = model,
    timestamp = timestamp,
    sessionId = SessionId("session-1"),
    promptId = PromptId("prompt-1"),
    tokenCounts =
        IncomingTokenCounts(
            inputTokens = TokenCount(100),
            outputTokens = TokenCount(200),
            cacheReadTokens = TokenCount(300),
            cacheWriteTokens = IncomingCacheWriteTokens(TokenCount.ZERO, TokenCount.ZERO, TokenCount(400)),
        ),
)
