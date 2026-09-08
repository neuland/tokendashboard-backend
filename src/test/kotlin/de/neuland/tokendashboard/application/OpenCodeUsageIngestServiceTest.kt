package de.neuland.tokendashboard.application

import arrow.core.nonEmptyListOf
import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.LlmProviderName
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.NanoCent
import de.neuland.tokendashboard.domain.model.OpenCodeUsageRecord
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptId
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
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

class OpenCodeUsageIngestServiceTest :
    FunSpec({

        val noUserRepo = mockk<UserRepositoryPort>()
        val repository = mockk<OpenCodeUsageRepositoryPort>()
        val noFactor = mockk<Co2FactorRepositoryPort>()

        fun service() = OpenCodeUsageIngestService(repository, noUserRepo, noFactor)

        beforeTest {
            clearAllMocks()
            every { noUserRepo.upsert(any(), any(), any()) } just Runs
            every { repository.insertAll(any()) } just Runs
            every { noFactor.co2FactorsFor(any(), any()) } returns null
        }

        test("ingest saves one record per prompt entry") {
            service().ingest(
                IngestOpenCodeUsageRecordCommand(
                    userId = UserId("user-1"),
                    prompts =
                        nonEmptyListOf(
                            promptEntry(PromptTimestamp(Instant.parse("2026-06-03T10:00:00Z"))),
                            promptEntry(PromptTimestamp(Instant.parse("2026-06-04T10:00:00Z"))),
                        ),
                    PluginVersion("1.2.3"),
                ),
            )

            verify(exactly = 1) { repository.insertAll(match { it.size == 2 }) }
        }

        test("ingest upserts user with open code provider") {
            service().ingest(
                IngestOpenCodeUsageRecordCommand(
                    userId = UserId("user-1"),
                    prompts = nonEmptyListOf(promptEntry(PromptTimestamp(Instant.parse("2026-06-04T10:00:00Z")))),
                    PluginVersion("1.2.3"),
                ),
            )

            verify { noUserRepo.upsert(any(), OPENCODE, any()) }
        }

        test("ingest maps all fields from command and defaults co2 to zero when no factor") {
            val slot = slot<List<OpenCodeUsageRecord>>()
            every { repository.insertAll(capture(slot)) } just Runs

            val entry =
                IngestOpenCodeUsageRecordCommand.Prompt(
                    model = ModelName.from("gpt-5.4-mini"),
                    llmProvider = LlmProviderName("openai"),
                    timestamp = PromptTimestamp(Instant.parse("2026-06-04T10:00:00Z")),
                    sessionId = SessionId("session-1"),
                    promptId = PromptId("prompt-1"),
                    tokenCounts =
                        TokenCounts(
                            inputTokens = TokenCount(100),
                            outputTokens = TokenCount(200),
                            cacheReadTokens = TokenCount(300),
                            cacheWriteTokens = TokenCount(400),
                        ),
                    cost = NanoCent.fromDollar(1.23),
                )
            service().ingest(
                IngestOpenCodeUsageRecordCommand(userId = UserId("u"), prompts = nonEmptyListOf(entry), PluginVersion("1.2.3")),
            )

            val record = slot.captured.single()
            record.model shouldBe ModelName.from("gpt-5.4-mini")
            record.llmProvider shouldBe LlmProviderName("openai")
            record.sessionId shouldBe SessionId("session-1")
            record.promptId shouldBe PromptId("prompt-1")
            record.tokens.inputTokens shouldBe TokenCount(100)
            record.tokens.outputTokens shouldBe TokenCount(200)
            record.tokens.cacheReadTokens shouldBe TokenCount(300)
            record.tokens.cacheWriteTokens shouldBe TokenCount(400)
            record.costNanoCent shouldBe NanoCent(123_000_000_000)
            record.co2Gram shouldBe Co2Gram(0.0)
            record.pluginVersion shouldBe PluginVersion("1.2.3")
        }
    })

private fun promptEntry(timestamp: PromptTimestamp) =
    IngestOpenCodeUsageRecordCommand.Prompt(
        model = ModelName.from("gpt-5.4-mini"),
        llmProvider = LlmProviderName("openai"),
        timestamp = timestamp,
        sessionId = SessionId("session-1"),
        promptId = PromptId("prompt-1"),
        tokenCounts =
            TokenCounts(
                inputTokens = TokenCount(100),
                outputTokens = TokenCount(50),
                cacheReadTokens = TokenCount(0),
                cacheWriteTokens = TokenCount(0),
            ),
        cost = NanoCent.fromDollar(0.01),
    )
