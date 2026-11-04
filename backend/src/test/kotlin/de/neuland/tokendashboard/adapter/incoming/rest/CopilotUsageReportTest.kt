package de.neuland.tokendashboard.adapter.incoming.rest

import de.neuland.tokendashboard.domain.model.ModelName
import de.neuland.tokendashboard.domain.model.NanoAiu
import de.neuland.tokendashboard.domain.model.SessionId
import de.neuland.tokendashboard.domain.model.TokenCount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class CopilotUsageReportTest :
    FunSpec({
        fun entry(
            sessionId: String = "session-1",
            model: String = "gpt-5-2026-01-01",
            inputTokens: Long = 100,
            outputTokens: Long = 200,
            cacheReadTokens: Long = 300,
            cacheWriteTokens: Long = 400,
            reasoningTokens: Long = 500,
            nanoAiu: Long = 42,
            requests: Int = 0,
        ) = CopilotUsageEntry(
            timestamp = "2026-06-15T10:00:00Z",
            sessionId = sessionId,
            model = model,
            usage =
                CopilotUsageTokens(
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    cacheReadTokens = cacheReadTokens,
                    cacheWriteTokens = cacheWriteTokens,
                    reasoningTokens = reasoningTokens,
                ),
            requests = requests,
            nanoAiu = nanoAiu,
        )

        test("maps every token field from the entry") {
            val report = CopilotUsageReport(userId = "user-1", entries = listOf(entry()), pluginVersion = "1.0.0")

            val prompt = report.toCommand().prompts.single()

            prompt.tokenCounts.inputTokens shouldBe TokenCount(100)
            prompt.tokenCounts.outputTokens shouldBe TokenCount(200)
            prompt.tokenCounts.cacheReadTokens shouldBe TokenCount(300)
            prompt.tokenCounts.cacheWriteTokens shouldBe TokenCount(400)
        }

        test("does not double count reasoning tokens by adding them to output tokens") {
            val report =
                CopilotUsageReport(
                    userId = "user-1",
                    entries = listOf(entry(outputTokens = 200, reasoningTokens = 500)),
                    pluginVersion = "1.0.0",
                )

            val prompt = report.toCommand().prompts.single()

            prompt.tokenCounts.outputTokens shouldBe TokenCount(200)
        }

        test("maps session id, model and nano aiu") {
            val report =
                CopilotUsageReport(
                    userId = "user-1",
                    entries = listOf(entry(sessionId = "session-42", model = "gpt-5-2026-01-01", nanoAiu = 99)),
                    pluginVersion = "1.0.0",
                )

            val prompt = report.toCommand().prompts.single()

            prompt.sessionId shouldBe SessionId("session-42")
            prompt.model shouldBe ModelName.from("gpt-5-2026-01-01")
            prompt.nanoAiu shouldBe NanoAiu(99)
        }

        test("rejects an empty prompt list") {
            val report = CopilotUsageReport(userId = "user-1", entries = emptyList(), pluginVersion = "1.0.0")

            val exception = shouldThrow<IllegalArgumentException> { report.toCommand() }

            exception.message shouldContain "prompts must not be empty"
        }

        test("rejects more prompts than the maximum") {
            val report =
                CopilotUsageReport(userId = "user-1", entries = List(MAX_PROMPTS_PER_REPORT + 1) { entry() }, pluginVersion = "1.0.0")

            val exception = shouldThrow<IllegalArgumentException> { report.toCommand() }

            exception.message shouldContain "too many prompts"
        }
    })
