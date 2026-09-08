package de.neuland.tokendashboard.integrationtests

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

private const val SONNET_ONE_DAY_CACHE_WRITE = 300 + 188 // 187.5 -> rounded 188
private const val HAIKU_ONE_DAY_CACHE_WRITE = 100 + 63 // 62.5 -> rounded 63
private const val SONNET_ONE_DAY_OTHER = 297 + 183
private const val HAIKU_ONE_DAY_OTHER = 99 + 61
const val SONNET_ONE_DAY = SONNET_ONE_DAY_OTHER + SONNET_ONE_DAY_CACHE_WRITE
const val HAIKU_ONE_DAY = HAIKU_ONE_DAY_OTHER + HAIKU_ONE_DAY_CACHE_WRITE

// each model, per day:
// 100.000 in, 100.000 out
// 1.000.000 cache write, 500.000 5min/1h
// 10.000.000 cache read
// sonnet and haiku,
suspend fun postClaudeJune14(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/claude") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user1",
              "plugin_version": "1.2.3",
              "prompts": [
                {
                  "entry_id": "claude-entry-1",
                  "timestamp": "2026-06-14T10:00:00.000Z",
                  "session_id": "session-1",
                  "model": "claude-sonnet-4-6",
                  "usage": {
                    "input_tokens": 40000,
                    "output_tokens": 70000,
                    "cache_creation_input_tokens": 500000,
                    "cache_read_input_tokens": 6000000
                  }
                },
                {
                  "entry_id": "claude-entry-2",
                  "timestamp": "2026-06-14T12:00:00.000Z",
                  "session_id": "session-1",
                  "model": "claude-sonnet-4-6",
                  "usage": {
                    "input_tokens": 60000,
                    "output_tokens": 30000,
                    "cache_creation_input_tokens": 500000,
                    "ephemeral_5m_input_tokens": 500000,
                    "ephemeral_1h_input_tokens": 0,
                    "cache_read_input_tokens": 4000000
                  }
                },
                {
                  "entry_id": "claude-entry-3",
                  "timestamp": "2026-06-14T10:00:00.000Z",
                  "session_id": "session-1",
                  "model": "claude-haiku-4-5",
                  "usage": {
                    "input_tokens": 40000,
                    "output_tokens": 70000,
                    "cache_creation_input_tokens": 500000,
                    "cache_read_input_tokens": 6000000
                  }
                },
                {
                  "entry_id": "claude-entry-4",
                  "timestamp": "2026-06-14T12:00:00.000Z",
                  "session_id": "session-1",
                  "model": "claude-haiku-4-5",
                  "usage": {
                    "input_tokens": 60000,
                    "output_tokens": 30000,
                    "cache_creation_input_tokens": 500000,
                    "ephemeral_5m_input_tokens": 500000,
                    "ephemeral_1h_input_tokens": 0,
                    "cache_read_input_tokens": 4000000
                  }
                }
              ]
            }
            """.trimIndent(),
        )
    }

suspend fun postClaudeJune15First(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/claude") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user2",
              "plugin_version": "1.2.3",
              "prompts": [
                {
                  "entry_id": "claude-entry-5",
                  "timestamp": "2026-06-15T10:00:00.000Z",
                  "session_id": "session-2",
                  "model": "claude-sonnet-4-6",
                  "usage": {
                    "input_tokens": 40000,
                    "output_tokens": 70000,
                    "cache_creation_input_tokens": 0,
                    "ephemeral_5m_input_tokens": 500000,
                    "ephemeral_1h_input_tokens": 50000,
                    "cache_read_input_tokens": 6000000
                  }
                },
                {
                  "entry_id": "claude-entry-6",
                  "timestamp": "2026-06-15T10:00:00.000Z",
                  "session_id": "session-2",
                  "model": "claude-haiku-4-5",
                  "usage": {
                    "input_tokens": 40000,
                    "output_tokens": 70000,
                    "cache_creation_input_tokens": 550000,
                    "ephemeral_5m_input_tokens": 500000,
                    "ephemeral_1h_input_tokens": 50000,
                    "cache_read_input_tokens": 6000000
                  }
                }
              ]
            }
            """.trimIndent(),
        )
    }

suspend fun postClaudeJune15Second(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/claude") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user3",
              "plugin_version": "1.2.3",
              "prompts": [
                {
                  "entry_id": "claude-entry-7",
                  "timestamp": "2026-06-15T12:00:00.000Z",
                  "session_id": "session-3",
                  "model": "claude-sonnet-4-6",
                  "usage": {
                    "input_tokens": 60000,
                    "output_tokens": 30000,
                    "cache_creation_input_tokens": 450000,
                    "ephemeral_1h_input_tokens": 400000,
                    "ephemeral_5h_input_tokens": 0,
                    "cache_read_input_tokens": 4000000
                  }
                },
                {
                  "entry_id": "claude-entry-8",
                  "timestamp": "2026-06-15T12:00:00.000Z",
                  "session_id": "session-3",
                  "model": "claude-haiku-4-5",
                  "usage": {
                    "input_tokens": 60000,
                    "output_tokens": 30000,
                    "cache_creation_input_tokens": 450000,
                    "ephemeral_1h_input_tokens": 400000,
                    "ephemeral_5h_input_tokens": 0,
                    "cache_read_input_tokens": 4000000
                  }
                }
              ]
            }
            """.trimIndent(),
        )
    }

suspend fun postClaudeWithoutPluginVersion(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/claude") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user1",
              "prompts": [
                {
                  "entry_id": "claude-entry-no-plugin-version",
                  "timestamp": "2026-06-14T10:00:00.000Z",
                  "session_id": "session-no-plugin-version",
                  "model": "claude-sonnet-4-6",
                  "usage": {
                    "input_tokens": 1,
                    "output_tokens": 2,
                    "cache_creation_input_tokens": 3,
                    "cache_read_input_tokens": 4
                  }
                }
              ]
            }
            """.trimIndent(),
        )
    }

suspend fun postClaudeMixedCacheWriteDurations(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/claude") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            object {}
                .javaClass
                .getResourceAsStream("/claude_usage_report_mixed_with_5m_1h.json")!!
                .bufferedReader()
                .use { it.readText() },
        )
    }
