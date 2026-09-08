package de.neuland.tokendashboard.adapter.incoming.rest

import de.neuland.tokendashboard.adapter.incoming.plugins.appJson
import de.neuland.tokendashboard.domain.model.PluginVersion
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val sampleEntry =
    ClaudeUsageEntry(
        timestamp = "2026-07-16T00:00:00Z",
        entryId = "entry-1",
        sessionId = "session-1",
        model = "claude-sonnet-4",
        usage =
            ClaudeUsageTokens(
                inputTokens = 1,
                outputTokens = 1,
                cacheCreationInputTokens = 0,
                cacheReadInputTokens = 0,
            ),
    )

class ClaudeUsageReportTest :
    FunSpec({

        test("toCommand throws when prompts exceed the cap") {
            val report =
                ClaudeUsageReport(
                    userId = "user-1",
                    entries = List(MAX_PROMPTS_PER_REPORT + 1) { sampleEntry },
                    pluginVersion = "1.0.0",
                )

            shouldThrow<IllegalArgumentException> { report.toCommand() }
        }

        test("toCommand does not throw at or below the cap") {
            val report =
                ClaudeUsageReport(
                    userId = "user-1",
                    entries = List(500) { sampleEntry },
                    pluginVersion = "1.0.0",
                )

            report.toCommand()
        }

        val prompt =
            """
            {
              "entry_id": "entry-1",
              "timestamp": "2026-06-14T10:00:00.000Z",
              "session_id": "session-1",
              "model": "claude-sonnet-4-6",
              "usage": {
                "input_tokens": 1,
                "output_tokens": 2,
                "cache_creation_input_tokens": 3,
                "cache_read_input_tokens": 4
              }
            }
            """.trimIndent()

        test("toCommand maps pluginVersion when present in payload") {
            val report =
                appJson.decodeFromString(
                    ClaudeUsageReport.serializer(),
                    """{"user_id": "user-1", "plugin_version": "1.2.3", "prompts": [$prompt]}""",
                )

            val command = report.toCommand()

            command.pluginVersion shouldBe PluginVersion("1.2.3")
        }

        test("an empty prompt list is rejected with a message") {
            val report = ClaudeUsageReport(userId = "user-1", entries = emptyList(), pluginVersion = "1.0.0")

            val exception = shouldThrow<IllegalArgumentException> { report.toCommand() }

            exception.message shouldContain "prompts"
        }
    })
