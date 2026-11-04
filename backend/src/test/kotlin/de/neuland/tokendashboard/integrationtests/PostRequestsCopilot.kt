package de.neuland.tokendashboard.integrationtests

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

// each model, per day:
// 100.000 in, 100.000 out
// 1.000.000 cache write
// 10.000.000 cache read
// gpt 5.4 and mini
suspend fun postCopilotJune14(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/copilot") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user1",
              "plugin_version": "1.2.3",
              "prompts": [
                {
                  "timestamp": "2026-06-14T10:00:00.000Z",
                  "session_id": "session-1",
                  "model": "gpt-5.4",
                  "usage": {
                    "input_tokens": 40000,
                    "output_tokens": 70000,
                    "cache_write_tokens": 550000,
                    "cache_read_tokens": 6000000,
                    "reasoning_tokens": 1000
                  },
                  "requests": 6,
                  "total_nano_aiu": 6006006006006,
                  "total_premium_requests": 2.97
                },
                {
                  "timestamp": "2026-06-14T12:00:00.000Z",
                  "session_id": "session-2",
                  "model": "gpt-5.4",
                  "usage": {
                    "input_tokens": 60000,
                    "output_tokens": 30000,
                    "cache_write_tokens": 450000,
                    "cache_read_tokens": 4000000,
                    "reasoning_tokens": 1000
                  },
                  "requests": 6,
                  "total_nano_aiu": 6006006006006,
                  "total_premium_requests": 1.33
                },
                {
                  "timestamp": "2026-06-14T10:00:00.000Z",
                  "session_id": "session-3",
                  "model": "gpt-5.4-mini",
                  "usage": {
                    "input_tokens": 40000,
                    "output_tokens": 70000,
                    "cache_write_tokens": 550000,
                    "cache_read_tokens": 6000000,
                    "reasoning_tokens": 1000
                  },
                  "requests": 1,
                  "total_nano_aiu": 4004004004004,
                  "total_premium_requests": 0.33
                },
                {
                  "timestamp": "2026-06-14T12:00:00.000Z",
                  "session_id": "session-4",
                  "model": "gpt-5.4-mini",
                  "usage": {
                    "input_tokens": 60000,
                    "output_tokens": 30000,
                    "cache_write_tokens": 450000,
                    "cache_read_tokens": 4000000,
                    "reasoning_tokens": 1000
                  },
                  "requests": 10,
                  "total_nano_aiu": 4004004004004,
                  "total_premium_requests": 0.33
                }
              ]
            }
            """.trimIndent(),
        )
    }

suspend fun postCopilotJune15First(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/copilot") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user2",
              "plugin_version": "1.2.3",
              "prompts": [
                {
                  "timestamp": "2026-06-15T10:00:00.000Z",
                  "session_id": "session-5",
                  "model": "gpt-5.4",
                  "usage": {
                    "input_tokens": 40000,
                    "output_tokens": 70000,
                    "cache_write_tokens": 550000,
                    "cache_read_tokens": 6000000,
                    "reasoning_tokens": 1000
                  },
                  "requests": 10,
                  "total_nano_aiu": 6006006006006,
                  "total_premium_requests": 0.33
                },
                {
                  "timestamp": "2026-06-15T10:00:00.000Z",
                  "session_id": "session-6",
                  "model": "gpt-5.4-mini",
                  "usage": {
                    "input_tokens": 40000,
                    "output_tokens": 70000,
                    "cache_write_tokens": 550000,
                    "cache_read_tokens": 6000000,
                    "reasoning_tokens": 1000
                  },
                  "requests": 5,
                  "total_nano_aiu": 4004004004004,
                  "total_premium_requests": 0.33
                }
              ]
            }
            """.trimIndent(),
        )
    }

suspend fun postCopilotJune15Second(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/copilot") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user3",
              "plugin_version": "1.2.3",
              "prompts": [
                {
                  "timestamp": "2026-06-15T12:00:00.000Z",
                  "session_id": "session-7",
                  "model": "gpt-5.4",
                  "usage": {
                    "input_tokens": 60000,
                    "output_tokens": 30000,
                    "cache_write_tokens": 450000,
                    "cache_read_tokens": 4000000,
                    "reasoning_tokens": 1000
                  },
                  "requests": 5,
                  "total_nano_aiu": 6006006006006,
                  "total_premium_requests": 0.33
                },
                {
                  "timestamp": "2026-06-15T12:00:00.000Z",
                  "session_id": "session-8",
                  "model": "gpt-5.4-mini",
                  "usage": {
                    "input_tokens": 60000,
                    "output_tokens": 30000,
                    "cache_write_tokens": 450000,
                    "cache_read_tokens": 4000000,
                    "reasoning_tokens": 1000
                  },
                  "requests": 5,
                  "total_nano_aiu": 4004004004004,
                  "total_premium_requests": 0.33
                }
              ]
            }
            """.trimIndent(),
        )
    }

suspend fun postCopilotSonnetAndHaiku(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/copilot") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user3",
              "plugin_version": "1.2.3",
              "prompts": [
                {
                  "timestamp": "2026-06-17T12:00:00.000Z",
                  "session_id": "session-9",
                  "model": "claude-sonnet-4.6",
                  "usage": {
                    "input_tokens": 6000,
                    "output_tokens": 100000,
                    "cache_write_tokens": 1000000,
                    "cache_read_tokens": 10000000,
                    "reasoning_tokens": 1000
                  },
                  "requests": 5,
                  "total_nano_aiu": 6006006006006,
                  "total_premium_requests": 0.33
                },
                {
                  "timestamp": "2026-06-17T12:00:00.000Z",
                  "session_id": "session-10",
                  "model": "claude-haiku-4.5",
                  "usage": {
                    "input_tokens": 600,
                    "output_tokens": 10000,
                    "cache_write_tokens": 450000,
                    "cache_read_tokens": 4000000,
                    "reasoning_tokens": 1000
                  },
                  "requests": 5,
                  "total_nano_aiu": 4004004004,
                  "total_premium_requests": 0.33
                }
              ]
            }
            """.trimIndent(),
        )
    }

suspend fun postCopilotWithoutPluginVersion(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/copilot") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user1",
              "prompts": [
                {
                  "timestamp": "2026-06-14T10:00:00.000Z",
                  "session_id": "session-no-plugin-version",
                  "model": "gpt-5.4-mini",
                  "usage": {
                    "input_tokens": 1,
                    "output_tokens": 2,
                    "cache_write_tokens": 3,
                    "cache_read_tokens": 4,
                    "reasoning_tokens": 5
                  },
                  "requests": 5,
                  "total_nano_aiu": 4004004004,
                  "total_premium_requests": 0.33
                }
              ]
            }
            """.trimIndent(),
        )
    }
