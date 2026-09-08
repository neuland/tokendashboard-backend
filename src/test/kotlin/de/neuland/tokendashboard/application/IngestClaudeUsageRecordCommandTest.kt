package de.neuland.tokendashboard.application

import arrow.core.nonEmptyListOf
import de.neuland.tokendashboard.domain.model.IncomingCacheWriteTokens
import de.neuland.tokendashboard.domain.model.IncomingTokenCounts
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptId
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.UserId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainOnly
import java.time.Instant

class IngestClaudeUsageRecordCommandTest :
    FunSpec({

        test("should filter out synthetik claude prompts") {
            val command =
                IngestClaudeUsageRecordCommand(
                    userId = userId,
                    prompts =
                        nonEmptyListOf(
                            promptUsage(ModelName.from("sonnet")),
                            promptUsage(ModelName.from("sonnet")),
                            promptUsage(ModelName.from("<synthetic>")),
                        ),
                    pluginVersion = PluginVersion("1.0.0"),
                )

            val result = command.nonSyntheticPrompts()

            result.map { it.model }.shouldContainOnly(ModelName.from("sonnet"))
        }
    })

private val userId = UserId("user-1")
private val someTokenCounts =
    IncomingTokenCounts(
        inputTokens = TokenCount(1),
        outputTokens = TokenCount(1),
        cacheReadTokens = TokenCount(0),
        cacheWriteTokens = IncomingCacheWriteTokens(TokenCount.ZERO, TokenCount.ZERO, TokenCount.ZERO),
    )

private fun promptUsage(modelName: ModelName) =
    IngestClaudeUsageRecordCommand.Prompt(
        model = modelName,
        timestamp = PromptTimestamp(Instant.parse("2024-06-15T10:00:00Z")),
        sessionId = SessionId("session-1"),
        promptId = PromptId("prompt-1"),
        tokenCounts = someTokenCounts,
    )
