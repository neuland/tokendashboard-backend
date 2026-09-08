package de.neuland.tokendashboard.application

import arrow.core.nonEmptyListOf
import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.CopilotUsageRecord
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.NanoAiu
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
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

class CopilotUsageIngestServiceTest :
    FunSpec({

        val noUserRepo = mockk<UserRepositoryPort>()
        val repository = mockk<CopilotUsageRepositoryPort>()
        val noFactor = mockk<Co2FactorRepositoryPort>()

        fun service() = CopilotUsageIngestService(repository, noUserRepo, noFactor)

        beforeTest {
            clearAllMocks()
            every { noUserRepo.upsert(any(), any(), any()) } just Runs
            every { noUserRepo.activeUsersLast4Weeks(any()) } returns UserCount(0)
            every { repository.insertAll(any()) } just Runs
            every { noFactor.co2FactorsFor(any(), any()) } returns null
        }

        test("ingest saves one record per prompt entry") {
            service().ingest(
                IngestCopilotUsageRecordCommand(
                    userId = UserId("user-1"),
                    prompts =
                        nonEmptyListOf(
                            promptEntry(PromptTimestamp(Instant.parse("2026-06-03T10:00:00Z"))),
                            promptEntry(PromptTimestamp(Instant.parse("2026-06-04T10:00:00Z"))),
                        ),
                    PluginVersion("1.0.0"),
                ),
            )

            verify(exactly = 1) { repository.insertAll(match { it.size == 2 }) }
        }

        test("ingest upserts with copilot provider") {
            service().ingest(
                IngestCopilotUsageRecordCommand(
                    userId = UserId("user-1"),
                    prompts = nonEmptyListOf(promptEntry(PromptTimestamp(Instant.parse("2026-06-04T10:00:00Z")))),
                    PluginVersion("1.0.0"),
                ),
            )

            verify { noUserRepo.upsert(any(), COPILOT, any()) }
        }

        test("ingest maps all token fields from command") {
            val slot = slot<List<CopilotUsageRecord>>()
            every { repository.insertAll(capture(slot)) } just Runs

            val entry =
                IngestCopilotUsageRecordCommand.Prompt(
                    model = ModelName.from("gpt-5.4-mini"),
                    timestamp = PromptTimestamp(Instant.parse("2026-06-04T10:00:00Z")),
                    sessionId = SessionId("session-1"),
                    tokenCounts =
                        TokenCounts(
                            inputTokens = TokenCount(100),
                            outputTokens = TokenCount(200),
                            cacheReadTokens = TokenCount(300),
                            cacheWriteTokens = TokenCount(400),
                        ),
                    nanoAiu = NanoAiu(123456789L),
                )
            service().ingest(
                IngestCopilotUsageRecordCommand(userId = UserId("u"), prompts = nonEmptyListOf(entry), PluginVersion("1.2.3")),
            )

            val record = slot.captured.single()
            record.model shouldBe ModelName.from("gpt-5.4-mini")
            record.tokens.inputTokens shouldBe TokenCount(100)
            record.tokens.outputTokens shouldBe TokenCount(200)
            record.tokens.cacheReadTokens shouldBe TokenCount(300)
            record.tokens.cacheWriteTokens shouldBe TokenCount(400)
            record.nanoAiu shouldBe NanoAiu(123456789L)
            record.pluginVersion shouldBe PluginVersion("1.2.3")
        }
    })

private fun promptEntry(timestamp: PromptTimestamp) =
    IngestCopilotUsageRecordCommand.Prompt(
        model = ModelName.from("gpt-5.4-mini"),
        timestamp = timestamp,
        sessionId = SessionId("session-1"),
        tokenCounts =
            TokenCounts(
                inputTokens = TokenCount(100),
                outputTokens = TokenCount(50),
                cacheReadTokens = TokenCount(0),
                cacheWriteTokens = TokenCount(0),
            ),
        nanoAiu = NanoAiu(1000000L),
    )
