# Domain-Driven Design Rules

No `Value<T>`/`Entity`/`AggregateRoot`/`Id` marker interfaces in this project — usage records are immutable data,
not entities with identity and lifecycle.

## Value Objects

- **`@JvmInline value class`** for a single wrapped primitive (`ModelName`, `PriceInCent`, `NanoAiu`, `UserId`, …).
- **`data class`** for compound values — multiple fields forming one concept (`DayRange`, `TokenCounts`,
  `ProviderUsage`, …).
- Types with validity constraints use a **private constructor + factory method** so an invalid instance can't be
  constructed (exemplar: `DayRange.of`).
- Prefer a `sealed interface` over a nullable/flag-based state for a small closed set of variants
  (`UsageResult.NoUsage` / `HasUsage(NonEmptyList<...>)` rules out "has usage but list is empty" at the type level
  and forces exhaustive `when`).

## Repositories

One driven port per data set, split across three files:

```
application/port/outgoing/ClaudeUsageRepositoryPort.kt    ← driven port (domain language, domain types only)
adapter/outgoing/repository/ClaudeUsageRepository.kt       ← JDBI SQL-object interface (@SqlQuery/@SqlBatch, row DTOs)
adapter/outgoing/repository/ClaudeUsageRepositoryAdapter.kt ← implements the port, translates rows into domain objects
```

`...RepositoryAdapter` is the anti-corruption layer: the only place that sees JDBI row types.

- Port interfaces return domain types only, never row types or DTOs.
- Methods express domain intent (`activeUser4Weeks()`, `claudeUsageSeries(from, to, granularity)`), not
  `findBy...`/`selectWhere...`.
- Naming: `<X>RepositoryPort` / `<X>Repository` (JDBI) / `<X>RepositoryAdapter`.

## Commands

See [architecture.md](architecture.md#commands).
