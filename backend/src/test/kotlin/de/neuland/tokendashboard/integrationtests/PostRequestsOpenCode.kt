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
// deepseek-v4-pro and mimo-v2.5-free
suspend fun postOpenCodeJune14(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/opencode") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user1",
              "plugin_version": "1.2.3",
              "prompts": [
                {
                  "entry_id": "b72bc0fa0af164f458e69a2a81df88ec00e77bc1",
                  "timestamp": "2026-06-14T10:00:00.000Z",
                  "session_id": "session-1",
                  "model": "deepseek-v4-pro",
                  "provider": "opencode-go",
                  "usage": {
                    "input_tokens": 40000,
                    "output_tokens": 20000,
                    "reasoning_tokens": 50000,
                    "cache_write_tokens": 550000,
                    "cache_read_tokens": 6000000
                  },
                  "cost": 0.01212121
                },
                {
                  "entry_id": "b72bc0fa0af164f458e69a2a81df88ec00e77bc2",
                  "timestamp": "2026-06-14T12:00:00.000Z",
                  "session_id": "session-1",
                  "model": "deepseek-v4-pro",
                  "provider": "opencode-go",
                  "usage": {
                    "input_tokens": 60000,
                    "output_tokens": 10000,
                    "reasoning_tokens": 20000,
                    "cache_write_tokens": 450000,
                    "cache_read_tokens": 4000000
                  },
                  "cost": 0.02121212
                },
                {
                  "entry_id": "b72bc0fa0af164f458e69a2a81df88ec00e77bc3",
                  "timestamp": "2026-06-14T10:00:00.000Z",
                  "session_id": "session-1",
                  "model": "mimo-v2.5-free",
                  "provider": "opencode-go",
                  "usage": {
                    "input_tokens": 40000,
                    "output_tokens": 45000,
                    "reasoning_tokens": 25000,
                    "cache_write_tokens": 550000,
                    "cache_read_tokens": 6000000
                  },
                  "cost": 0.01212121
                },
                {
                  "entry_id": "b72bc0fa0af164f458e69a2a81df88ec00e77bc4",
                  "timestamp": "2026-06-14T12:00:00.000Z",
                  "session_id": "session-1",
                  "model": "mimo-v2.5-free",
                  "provider": "opencode-go",
                  "usage": {
                    "input_tokens": 60000,
                    "output_tokens": 9000,
                    "reasoning_tokens": 21000,
                    "cache_write_tokens": 450000,
                    "cache_read_tokens": 4000000
                  },
                  "cost": 0.02121212
                }
              ]
            }
            """.trimIndent(),
        )
    }

suspend fun postOpenCodeJune15First(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/opencode") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user2",
              "plugin_version": "1.2.3",
              "prompts": [
                {
                  "entry_id": "b72bc0fa0af164f458e69a2a81df88ec00e77bc5",
                  "timestamp": "2026-06-15T10:00:00.000Z",
                  "session_id": "session-2",
                  "model": "deepseek-v4-pro",
                  "provider": "opencode-go",
                  "usage": {
                    "input_tokens": 40000,
                    "output_tokens": 49000,
                    "reasoning_tokens": 21000,
                    "cache_write_tokens": 550000,
                    "cache_read_tokens": 6000000
                  },
                  "cost": 0.02121212
                },
                {
                  "entry_id": "b72bc0fa0af164f458e69a2a81df88ec00e77bc6",
                  "timestamp": "2026-06-15T10:00:00.000Z",
                  "session_id": "session-2",
                  "model": "mimo-v2.5-free",
                  "provider": "opencode-go",
                  "usage": {
                    "input_tokens": 40000,
                    "output_tokens": 50000,
                    "reasoning_tokens": 20000,
                    "cache_write_tokens": 550000,
                    "cache_read_tokens": 6000000
                  },
                  "cost": 0.01212121
                }
              ]
            }
            """.trimIndent(),
        )
    }

suspend fun postOpenCodeJune15Second(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/opencode") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user3",
              "plugin_version": "1.2.3",
              "prompts": [
                {
                  "entry_id": "b72bc0fa0af164f458e69a2a81df88ec00e77bc7",
                  "timestamp": "2026-06-15T12:00:00.000Z",
                  "session_id": "session-3",
                  "model": "deepseek-v4-pro",
                  "provider": "opencode-go",
                  "usage": {
                    "input_tokens": 60000,
                    "output_tokens": 10000,
                    "reasoning_tokens": 20000,
                    "cache_write_tokens": 450000,
                    "cache_read_tokens": 4000000
                  },
                  "cost": 0.02121212
                },
                {
                  "entry_id": "b72bc0fa0af164f458e69a2a81df88ec00e77bc8",
                  "timestamp": "2026-06-15T12:00:00.000Z",
                  "session_id": "session-3",
                  "model": "mimo-v2.5-free",
                  "provider": "opencode-go",
                  "usage": {
                    "input_tokens": 60000,
                    "output_tokens": 10000,
                    "reasoning_tokens": 20000,
                    "cache_write_tokens": 450000,
                    "cache_read_tokens": 4000000
                  },
                  "cost": 0.01212121
                }
              ]
            }
            """.trimIndent(),
        )
    }

suspend fun postOpenCodeEmptyQwen(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/opencode") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user3",
              "plugin_version": "1.2.3",
              "prompts": [
                {
                  "entry_id": "537726068da9c0d5695a72708187761d9dce58a9",
                  "timestamp": "2026-06-15T12:00:00.000Z",
                  "plugin_version": "0.1.2",
                  "session_id": "ses_05da97eb5ffevySX86Zs47Egcn",
                  "model": "qwen",
                  "provider": "ollama",
                  "usage": {
                    "input_tokens": 0,
                    "output_tokens": 0,
                    "reasoning_tokens": 0,
                    "cache_read_tokens": 0,
                    "cache_write_tokens": 0
                  },
                  "cost": 0
                }
              ]
            }
            """.trimIndent(),
        )
    }

suspend fun postOpenCodeWithoutPluginVersion(client: HttpClient): HttpResponse =
    client.post("/api/usage/ingest/opencode") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(
            """
            {
              "user_id": "user1",
              "prompts": [
                {
                  "entry_id": "open-code-entry-no-plugin-version",
                  "timestamp": "2026-06-14T10:00:00.000Z",
                  "session_id": "session-no-plugin-version",
                  "model": "qwen",
                  "provider": "ollama",
                  "usage": {
                    "input_tokens": 1,
                    "output_tokens": 2,
                    "reasoning_tokens": 3,
                    "cache_read_tokens": 4,
                    "cache_write_tokens": 5
                  },
                  "cost": 0
                }
              ]
            }
            """.trimIndent(),
        )
    }
