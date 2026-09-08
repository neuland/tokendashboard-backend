# TokenDashboard

The project aims to visualise the company’s token usage across various providers.
This repo is the **backend only** (no frontend). It is written in Kotlin (ktor) and
**receives usage data pushed** via `POST /api/usage/ingest/*` — it does not poll
provider APIs (see decision #1). Postgres stores the data; a separate frontend
(out of scope for this repo) consumes the aggregated `GET /api/usage/*` endpoints.

The following providers are used:

- Claude Code
- GitHub Copilot
- OpenCode

## API

| Method | Path                            | Query params                                                                                     | Purpose                                                                                                                                                                                                           |
|--------|---------------------------------|--------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GET    | `/health`                       | –                                                                                                | Health check incl. database ping: `200 {"status": "ok"}`, `503 {"status": "error"}` when the database is unreachable                                                                                             |
| GET    | `/api/usage/claude/v1`          | `from`, `to` (ISO, required; must be ordered and span ≤ 5 years → `400`)                         | Claude aggregate over the range: tokens/cost/CO2 per model, plus `activeUsersLast4Weeks`                                                                                                                          |
| GET    | `/api/usage/claude/series/v1`   | `from`, `to` (as above), `granularity` (`day` or `week`, default `day`; any other value → `400`) | Claude usage as a time series, one bucket per `granularity` unit with usage (sparse — units without usage are omitted), per bucket: model breakdown, `overallTotalTokens`, `pluginInstallations`. No range limit. |
| GET    | `/api/usage/copilot/v1`         | `from`, `to`                                                                                     | Copilot aggregate over the range, analogous to `/api/usage/claude/v1`                                                                                                                                             |
| GET    | `/api/usage/copilot/series/v1`  | `from`, `to`, `granularity` (as `/api/usage/claude/series/v1`)                                   | Copilot usage as a time series, analogous to `/api/usage/claude/series/v1`                                                                                                                                        |
| GET    | `/api/usage/opencode/v1`        | `from`, `to`                                                                                     | OpenCode aggregate over the range, analogous to `/api/usage/claude/v1`                                                                                                                                            |
| GET    | `/api/usage/opencode/series/v1` | `from`, `to`, `granularity` (as `/api/usage/claude/series/v1`)                                   | OpenCode usage as a time series, analogous to `/api/usage/claude/series/v1`                                                                                                                                       |
| GET    | `/api/usage/all/v1`             | `from`, `to`                                                                                     | Aggregate across all providers (Claude + Copilot + OpenCode)                                                                                                                                                      |
| GET    | `/api/usage/all/series/v1`      | `from`, `to`, `granularity` (as above)                                                           | Time series across all providers; each bucket carries a per-provider breakdown plus summed `overallTotalTokens` and `pluginInstallations`                                                                         |
| POST   | `/api/usage/ingest/claude`      | – (body: `ClaudeUsageReport`)                                                                    | Accepts Claude usage data from the plugin                                                                                                                                                                         |
| POST   | `/api/usage/ingest/copilot`     | – (body: `CopilotUsageReport`)                                                                   | Accepts Copilot usage data from the plugin                                                                                                                                                                        |
| POST   | `/api/usage/ingest/opencode`    | – (body: `OpenCodeUsageReport`)                                                                  | Accepts OpenCode usage data from the plugin                                                                                                                                                                       |
| GET    | `/api/prices/claude`            | -                                                                                                | Returns current prices for claude                                                                                                                                                                                 |

`from` and `to` are required on every `GET /api/usage/*` endpoint; a missing parameter
yields `400`. All `from`/`to` pairs are validated by `DayRange.of`: unordered or spanning
more than `DayRange.MAX_RANGE_DAYS` (5 years) → `400`. All ingest endpoints check the body
size before parsing: a declared size above `MAX_REQUEST_BODY_BYTES` (8 MiB) → `413`, a
missing `Content-Length` → `411`.

Source of truth is `Routing.kt` — keep this table in sync when endpoints change.

## Decisions

See [decisions.md](../../docs/decisions.md) for business/domain decisions and their reasoning (privacy constraints,
provider scope, CO2 calculation method, etc.).
