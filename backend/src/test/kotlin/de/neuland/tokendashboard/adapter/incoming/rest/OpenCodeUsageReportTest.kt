package de.neuland.tokendashboard.adapter.incoming.rest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

private val sampleEntry =
    OpenCodeUsageEntry(
        timestamp = "2026-06-15T10:00:00Z",
        entryId = "entry-1",
        sessionId = "session-1",
        model = "claude-sonnet-4-5",
        provider = "anthropic",
        usage =
            OpenCodeUsageTokens(
                inputTokens = 1,
                outputTokens = 1,
                cacheWriteTokens = 0,
                cacheReadTokens = 0,
                reasoningTokens = 0,
            ),
        cost = 0.0,
    )

class OpenCodeUsageReportTest :
    FunSpec({

        test("toCommand throws when prompts exceed the cap") {
            val report =
                OpenCodeUsageReport(
                    userId = "user-1",
                    entries = List(MAX_PROMPTS_PER_REPORT + 1) { sampleEntry },
                    pluginVersion = "1.0.0",
                )

            shouldThrow<IllegalArgumentException> { report.toCommand() }
        }

        test("toCommand does not throw at or below the cap") {
            val report =
                OpenCodeUsageReport(
                    userId = "user-1",
                    entries = List(500) { sampleEntry },
                    pluginVersion = "1.0.0",
                )

            report.toCommand()
        }

        test("an empty prompt list is rejected with a message") {
            val report = OpenCodeUsageReport(userId = "user-1", entries = emptyList(), pluginVersion = "1.0.0")

            val exception = shouldThrow<IllegalArgumentException> { report.toCommand() }

            exception.message shouldContain "prompts"
        }
    })
