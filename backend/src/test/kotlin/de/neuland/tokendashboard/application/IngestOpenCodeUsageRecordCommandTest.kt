package de.neuland.tokendashboard.application

import arrow.core.nonEmptyListOf
import de.neuland.tokendashboard.domain.model.LlmProviderName
import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.NanoCent
import de.neuland.tokendashboard.domain.model.PluginVersion
import de.neuland.tokendashboard.domain.model.PromptId
import de.neuland.tokendashboard.domain.model.PromptTimestamp
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCount
import de.neuland.tokendashboard.domain.model.TokenCounts
import de.neuland.tokendashboard.domain.model.UserId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class IngestOpenCodeUsageRecordCommandTest :
    FunSpec({
        test("should filter out empty prompts") {
            val command =
                IngestOpenCodeUsageRecordCommand(
                    userId = userId,
                    prompts =
                        nonEmptyListOf(
                            prompt(zeroTokenCounts),
                            prompt(someTokenCounts),
                        ),
                    pluginVersion = PluginVersion("1.0.0"),
                )

            val result = command.nonZeroPrompts()

            result.map { it.model }.size shouldBe 1
            result.filter { it.tokenCounts.sumOverAll().value == 0L } shouldBe emptyList()
        }
    })

private val userId = UserId("user-1")
private val someTokenCounts =
    TokenCounts(
        inputTokens = TokenCount(100),
        outputTokens = TokenCount(0),
        cacheReadTokens = TokenCount(0),
        cacheWriteTokens = TokenCount(0),
    )
private val zeroTokenCounts =
    TokenCounts(
        inputTokens = TokenCount(0),
        outputTokens = TokenCount(0),
        cacheReadTokens = TokenCount(0),
        cacheWriteTokens = TokenCount(0),
    )

private fun prompt(tokenCounts: TokenCounts) =
    IngestOpenCodeUsageRecordCommand.Prompt(
        model = ModelName.from("qwen"),
        llmProvider = LlmProviderName("ollama"),
        timestamp = PromptTimestamp(Instant.parse("2024-06-15T10:00:00Z")),
        sessionId = SessionId("session-1"),
        promptId = PromptId("prompt-1"),
        tokenCounts = tokenCounts,
        cost = NanoCent(1L),
    )
