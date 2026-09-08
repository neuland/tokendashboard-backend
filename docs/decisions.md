# Decisions

Business/domain decisions and their reasoning. Linked from CLAUDE.md and
loaded into agent context.

## 1. Plugin-based approach per provider

Claude is the strongest-usage provider, and its subscription plan doesn't
expose tokens via API — only pay-as-you-go plans do, which isn't our setup.
A plugin architecture handles this and other provider-specific constraints
independently instead of relying on a single uniform API integration.

## 2. Free write tokens are counted

Free write tokens (e.g. OpenAI cache writes) are counted in the dashboard
even though they don't directly contribute to cost:

- Part of overall token usage.
- Gives a complete picture of system usage for future optimization.
- May carry indirect costs (latency, resource usage, CO2) not captured by
  billing.
- Pricing models may start billing them later; historical data is only
  available if collected from the start.

## 3. Derivation of CO₂ factors

See README.md.

## 4. No authentication, no user/team attribution

No authentication; reachable only via VPN. Sender of usage data 
cannot be traced on the data level. Prompt content is never collected or
sent to the backend — only token counts.

Deliberate data-minimization choice. Any feature needing per-user or
per-team granularity (e.g. "aggregate by team") requires a separate,
dedicated decision to introduce auth infrastructure.

The backend stores the ingest `user_id` in a `users` table, solely to count
active installations per provider (`last_data_sent`, `provider`). Usage
records carry no `user_id` — token/cost/CO2 data is never linked to an
individual.

## 5. Claude prices: single source via endpoint, not a duplicated file

The Claude plugin needs the same per-model prices (`claude_prices` table) as
the backend, to show users their own costs. `GET /api/prices/claude` returns
the currently valid price per model family straight from the table.

## 6. Series return number of installations, not users at bucket day

**`pluginInstallations` semantics**: number of installations "active" on
the bucket day, defined as:

```
first_data_sent <= day AND day < last_data_sent + 4 weeks
```

- Counts cumulative installations within the 4-week window from the last
  data transmission, not "how many sent data on that day".
- No drop-offs or re-entries are tracked: installing on Monday and
  re-installing on Wednesday (new `id`) counts on both days.
  Intentional; traceability is not intended.

## 7. Only GET routes are versioned (`.../v1` suffix); POST ingest and the
   plugin-facing prices GET are not

The plugins (Claude, Copilot, OpenCode) each live in their own separate
repository, one per plugin, since they have fundamentally different data
sources and cannot report the same data as each other. Within each plugin's
repository, every org's install of that plugin is nonetheless the same
build, while each org's backend is deployed independently, on its own
schedule. Frontend↔backend
and plugin↔backend routes are versioned differently as a result:

- **Frontend ↔ backend GET routes.** Frontend and backend of one deployment
  are versioned and deployed together by that org, so response shapes can
  change freely. These GET routes carry a `.../v1` suffix (e.g.
  `/api/usage/claude/v1`, `/api/usage/all/series/v1`) so a future breaking
  change can bump just that route to `.../v2` without touching the others.

- **Plugin ↔ backend routes.** The plugin build is shared and not forked
  per org, so it cannot hardcode a version: a bumped
  `/api/usage/ingest/claude` route would instantly break every org still on
  the old backend. These routes (`POST
  /api/usage/ingest/{claude,copilot,opencode}`, and `GET
  /api/prices/claude`, which the Claude plugin also calls) stay unversioned
  and additive-only forever: only add optional fields, never remove or
  rename. This keeps the plugin usable straight from GitHub, with zero
  per-org adjustment.

Each versioned route carries its own `.../v1` suffix at the end of its
path, not one version segment wrapping the whole API.
A breaking change to one route only bumps that route; 
every other route stays untouched. `/health` stays unversioned
(infra probe, not part of the plugin contract).

Path suffix chosen over header/media-type versioning: visible and testable
without extra tooling — an operator running their own fork can see the
version in the URL/logs/curl output directly.

## 8. Ephemeral cache tokens: store the undifferentiated remainder as its own value; bill it at the 1h rate

Claude usage reports carry both a flat `cache_creation_input_tokens` total and,
optionally, a split into `ephemeral_5m_input_tokens` / `ephemeral_1h_input_tokens`.
Sometimes the sum falls short of the total. 

We store this shortfall as its own "generic" value in the database, rather
than folding it silently into the 5m or 1h count. For pricing/CO2 purposes it
is billed at the 1h rate (in code, not via a dedicated price column).
Adjust this (for price calculation as well as the /prices/claude-API) if another
value fits your system better.

Why:
- We want the dashboard to stay simple: one price per token type, no
  "unknown" bucket exposed to users. This is a deliberate simplification, not
  an attempt to be maximally accurate.
- We do not know which TTL the generic tokens actually belong to, and the
  ratio of 5m to 1h usage is provider/setup-specific (e.g. it depends on how
  a given CLI sets `cache_control.ttl`). A hard 1h default that fits our own
  usage pattern may not fit other setups.
- Storing the generic amount separately (instead of just adding it into the
  1h count) keeps this assumption visible and correctable later, without
  losing historical data. 
- Billing it in code (rather than via a separate `cache_write_cent_per_million`
  price column for "generic") avoids implying that a dedicated, meaningfully
  different price exists for this case. 
- By using the 1h value for the `/api/prices/claude` API, we keep the plugin
  free of logic. If your system uses a different price, change it here too,
  so the plugin's status line shows your price.

## 9. Cost stored as cents (`BIGINT`), CO₂ stored as grams (`DOUBLE PRECISION`)

Costs and CO₂ are estimates, not billing-grade figures — pricing and CO₂
factors are themselves approximations (see decision 3), and the dashboard's
purpose is trend visibility, not exact accounting.

- **Cost**: stored as whole cents (`BIGINT`, banker's rounding at ingest).
  Not scaled to sub-cent units; very cheap requests may round to 0 cents,
  which is accepted. Chosen over `NUMERIC`/`DOUBLE PRECISION` for
  human-readable values in the DB and exact, simple integer addition when
  aggregating.
- **CO₂**: stored in grams as `DOUBLE PRECISION`, since some records fall
  below 1 gram and an integer type is not viable. `NUMERIC` was considered
  but rejected: the main use case is aggregation, and exactness is
  explicitly not a goal here either — the underlying CO₂ factors are
  approximations, so `NUMERIC`'s exact-decimal guarantees would not reflect
  any real precision in the data.

If a future need for finer-grained cost tracking arises (e.g. per-token
pricing well below a cent, or a business requirement for exact billing),
revisit the cents/`BIGINT` choice rather than silently rounding harder.

## 10. Missing price/CO₂ factor defaults to 0, not a distinct "unknown" state

When a model/day has no matching entry in the price or CO₂ factor table
(e.g. a new model was used before its price was added), `ClaudeUsageIngestService`
stores `costUsdCent`/`co2Gram` as `0` rather than modeling "unknown" as its own
type. A `logger.warn` is emitted at ingest time.

This is deliberate, not an oversight:

- The dashboard breaks usage down per model, so a model showing "0 cost/CO2"
  while clearly having usage is self-evidently wrong and gets noticed without
  needing a dedicated "unknown" indicator.
- The fix is operational, not code: look up the correct price/factor, insert it
  into the `claude_prices`/`co2_factors` table, and re-ingest or backfill the
  affected historical records for that model.
- Modeling "unknown" as a distinct type would ripple into every consumer
  (aggregation, serialization, dashboard display) for a rare, self-correcting
  operational gap — not worth the complexity (see `.claude/system/codestyle.md`,
  "Pragmatism > Dogma").

## 11. Cost figures are estimates; per-model and total rounding may differ

This applies only to providers that store cost in nano-precision (see
decision 12) — currently OpenCode and Copilot, not Claude. For those
providers, the *total* returned by the all-providers endpoint is computed by
summing nano-precision cost across all matching records and rounding once,
at the end. The *per-model* breakdown that each provider's own
single-provider endpoint sums into its own overall total instead rounds cost
to whole cents per model row first, and only then sums those already-rounded
values.

As a consequence, **the per-provider endpoint and the all-providers endpoint
can report different cost figures for the same date range**, differing by up
to roughly half a cent per model. The test suite encodes this for OpenCode:
the all-providers total and the single-provider total for the same
underlying data are expected to differ by a cent, with a `// rounding`
comment marking the divergence as intentional. Claude cannot diverge this
way: its per-record cost is already a whole cent at ingest, so there is no
nano intermediate to round twice — summing the same whole-cent values in any
grouping always produces the same total.

This is accepted, not an oversight to fix:

- The figures are order-of-magnitude estimates for internal reporting,
  consistent with the CO2 estimate caveat already noted in README.md.
  Cent-level precision across endpoints is not a goal.
- Unifying the two rounding paths would require summing in nano-precision
  everywhere and rounding once at the very end, including for per-model
  figures. If the rounding is ever unified, this entry should be updated or removed.

## 12. Per-provider cost precision: whole cents vs. nano-precision

Providers store per-prompt cost at different precisions, and for different
reasons — this is not one rule applied three times, it is three separate
calls. A shared constraint underlies all three: cost is always stored as an
integer column, never as floating point, to avoid the rounding
inconsistencies float storage would introduce. What differs is which integer
granularity each provider's column uses.

**Claude** rounds the cost of a single prompt to a whole cent at ingest time
and stores it as an integer cent column. This is safe because Claude's
per-prompt costs are almost always well above a cent — whole-cent rounding
loses nothing meaningful there.

**OpenCode** stores cost as `NanoCent`, at a billion-times-finer resolution
than a cent. Here a meaningful share of individual prompts cost less than
one cent; rounding such a prompt to the nearest whole cent at ingest would
store it as `0` and permanently discard cost that was actually incurred.
Nano-precision keeps that value intact until it is summed with others, where
it can add up to something significant.

**Copilot** stores cost as `NanoAiu`, for a third, independent reason: the
plugin already reports the cost from GitHub's own billing unit (the
AI-Unit) at that same nano-scale precision, as an integer. Storing it
as-is avoids an unnecessary rounding step, and keeps the AI-Unit available
for other uses later — for example, if a CO2 factor is ever defined per
AI-Unit. Whether Copilot's own per-prompt costs frequently fall below a cent
is not the deciding factor here, unlike for OpenCode.

A future provider should be evaluated on its own terms — its own per-prompt
cost distribution, and the precision its own source data already provides —
not assumed to match any existing provider's reasoning.

This is a different situation from decision 10 (missing price/CO₂ factor
defaults to 0): there, the price is *absent* from the lookup table. Here,
the price is present and applied correctly — the cost is genuinely below the
rounding threshold and rounds down to a stored `0`. Do not go looking for a
missing price row for this case; there isn't one.

## 13. Copilot's `reasoning_tokens` are parsed but not added to output tokens; OpenCode's are

Copilot's ingest payload includes a `reasoning_tokens` field per prompt, but
it is intentionally not added into the stored output-token count. OpenCode's
equivalent field *is* added to its output-token count.

This asymmetry is correct: Copilot already includes reasoning tokens in its
reported output-token total, so adding them again would double-count.
OpenCode reports reasoning tokens separately from its output-token total, so
they must be added explicitly to get the true output-token count. The
`reasoning_tokens` field on the Copilot side is parsed because it is present
in every payload, and then deliberately left unused.

## 14. Copilot deduplicates on `session_id` alone — a cross-repo dependency

Copilot's ingest insert uses `ON CONFLICT (session_id) DO NOTHING`, backed by
a unique index on `session_id` alone. Claude and OpenCode both dedupe on the
wider key `(session_id, prompt_id)`.

The narrow key is safe **only because of a plugin-side invariant**: the
Copilot plugin sends each session exactly once, as a session-aggregated
cumulative total. Copilot payloads carry no per-prompt id, unlike Claude and
OpenCode, so no narrower key is available on the backend side even if one
were wanted.

This is a **cross-repo dependency**, and the failure mode it protects
against must be spelled out: if the Copilot plugin is ever changed to
re-send an open session with updated cumulative totals, `DO NOTHING` will
silently discard the update, and that session's usage will stay frozen at
its first-reported value — with no error raised anywhere, and a normal
success response returned to the plugin. Should the Copilot plugin ever
start re-sending open sessions, the conflict key here must widen to include
a per-prompt identifier *first*, before that plugin change ships. Nothing in
the plugin's own repository currently hints that this backend assumption
exists.
