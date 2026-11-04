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

See co2-methodology.md.

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

For the plugin's local usage history, `GET /api/prices/claude/history` and
`GET /api/co2-factors/claude` return every version of both tables with its
`validFrom` date. Local entries span price changes (sonnet changed on
2026-08-12), so current values alone would misprice older days. The family
names are the keys of both routes, which is also how the plugin maps a model
id to its family — no second copy of the mapping rule.

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
  /api/prices/claude`, `/api/prices/claude/history` and
  `/api/co2-factors/claude`, which the Claude plugin also calls) stay unversioned
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

## 9. Cost and CO₂ are calculated on read, not stored

Usage records store only what the plugins report: token counts, plus the
provider's own raw cost value where one exists (see decision 12). Cost and CO₂
are calculated when usage is read, from the price and CO₂ factor tables
valid on each usage day. Correcting a price or factor therefore applies
retroactively, without touching usage records.

Costs and CO₂ are estimates, not billing-grade figures — pricing and CO₂
factors are themselves approximations (see decision 3), and the dashboard's
purpose is trend visibility, not exact accounting.

- **Cost**: whole cents (see decision 11). Very cheap usage may round to 0
  cents, which is accepted.
- **CO₂**: grams as a floating-point value, since daily usage of a model can
  fall below 1 gram. Exact decimals were rejected: the underlying CO₂ factors
  are approximations, so exactness would not reflect any real precision.

If a future need for finer-grained cost tracking arises (e.g. a business
requirement for exact billing), revisit the whole-cent rounding rather than
silently rounding harder.

## 10. Missing price/CO₂ factor defaults to 0, not a distinct "unknown" state

Prices and CO₂ factors are versioned per model family by `validFrom` date.
For a usage day, the latest version valid on that day applies. If the day
lies before the oldest version, the oldest version applies: a model's list
price is assumed to hold from its release on. A model family without any
price or factor gets cost/CO₂ `0`, rather than modeling "unknown" as its own
type.

This is deliberate, not an oversight:

- The dashboard breaks usage down per model, so a model showing "0 cost/CO2"
  while clearly having usage is self-evidently wrong and gets noticed without
  needing a dedicated "unknown" indicator.
- The fix is operational, not code: insert the correct price/factor into the
  `claude_prices`/`co2_factors` table. Since cost and CO₂ are calculated on
  read (decision 9), this also corrects all historical figures.
- Modeling "unknown" as a distinct type would ripple into every consumer
  (aggregation, serialization, dashboard display) for a rare, self-correcting
  operational gap — not worth the complexity (see `.claude/system/codestyle.md`,
  "Pragmatism > Dogma").

## 11. Cost is rounded to whole cents per day and model, then summed

Every provider calculates cost per usage day and model and rounds it to a
whole cent. All totals — per model over a range, per provider, across all
providers — sum these already-rounded values. Every endpoint therefore
reports the same figure for the same data, regardless of grouping.

Rounding once at the very end would be marginally more precise, but makes
per-model figures and totals diverge by fractions of a cent per model. The
figures are estimates (decision 9); consistency across endpoints is worth
more than sub-cent precision.

## 12. Per-provider raw cost: none, or the plugin's own nano-precision value

Providers differ in whether and how they report cost; each is a separate call.
Where a raw cost value is stored, it is an integer column, never floating
point, to avoid float rounding inconsistencies.

**Claude** stores no cost. Its cost is calculated from token counts and the
`claude_prices` table (decision 9).

**OpenCode** stores the plugin-reported cost as `NanoCent`, at a
billion-times-finer resolution than a cent. A meaningful share of individual
prompts cost less than one cent; rounding each prompt to a whole cent would
turn it into `0` and discard cost that was actually incurred. Nano-precision
keeps that value intact until it is summed per day and model (decision 11).

**Copilot** stores cost as `NanoAiu`: the plugin already reports GitHub's own
billing unit (the AI-Unit) at nano-scale precision, as an integer. Storing it
as-is avoids an unnecessary conversion, and keeps the AI-Unit available for
other uses later — for example, if a CO2 factor is ever defined per AI-Unit.

A future provider should be evaluated on its own terms — its own per-prompt
cost distribution, and the precision its own source data already provides —
not assumed to match any existing provider's reasoning.

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
## 15. Day boundaries follow one configurable reporting time zone per deployment

Usage is grouped into days in a single time zone, set per deployment via
`REPORTING_TIME_ZONE` (an IANA zone id, default `UTC`).

- **One zone per deployment, not per viewer.** Everyone opening the
  dashboard sees the same days and therefore the same figures, wherever they
  are. Per-viewer zones would make totals depend on who is looking.
- **Configurable rather than fixed to UTC.** A team in a time zone far from
  UTC would otherwise see its working day split across two dashboard days.
  The zone should match where most of the deployment's users work.

Because cost and CO₂ are calculated on read (decision 9), changing the zone
takes effect after a restart and regroups all historical usage as well;
nothing has to be migrated.

## 18. Total tokens count input and output only; cache tokens are shown separately

The headline "total tokens" figure — company-wide and per provider — sums
input and output tokens. Cache-write and cache-read tokens are excluded from
it, but stored and shown separately: in the breakdown by token type, in the
per-model table and as their own chart views.

Why: agentic coding re-reads large repository contexts thousands of times, so
cache reads outnumber input and output by one to two orders of magnitude.
Summed into one figure they would push the total into an extreme range,
flatten real fluctuations into noise and make the figure incomparable over
time. Excluding them keeps the total meaningful; keeping them visible
elsewhere means nothing is dropped.

## 19. Copilot cost is approximated at one US cent per AI unit

Copilot cost is computed as 1 US cent per AI unit, from the AI-unit total
the Copilot CLI reports per session (stored as `NanoAiu`, see decision 12).

Why: GitHub's pricing communication is ambiguous. One cent per AI *credit*
(not unit) is documented only for the credit-based plans, which are
deprecated; precise pricing for the usage-based plans is not yet available.
Until it is, the approximation is used and labelled as such in the
dashboard's FAQ. Storing the raw AI units keeps the quantity intact for a
later, exact conversion — or for a CO₂ factor per AI unit, should one become
available — and the historical data can then be recomputed.
